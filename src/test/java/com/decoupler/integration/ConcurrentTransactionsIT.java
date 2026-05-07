package com.decoupler.integration;

import com.decoupler.impl.DefaultProxy;
import com.decoupler.impl.sql.SqlReadCommand;
import com.decoupler.impl.sql.SqlTransaction;
import com.decoupler.impl.sql.SqlResult;
import com.decoupler.interfaces.*;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates that the routing layer handles concurrent transactions correctly.
 *
 * <p>No Ollama dependency — a stub {@link Aggregator} covers the DECOUPLED path.
 *
 * <p><b>Thread-safety notes:</b>
 * <ul>
 *   <li>{@code DefaultProxy.statusMap} (ConcurrentHashMap) and {@code log} (CopyOnWriteArrayList)
 *       are safe for concurrent access.
 *   <li>{@code SqlPartitioner.cachedFragments} lazy-init is NOT synchronized — safe here only
 *       because {@code proxy.init()} completes single-threaded in {@code @BeforeAll} before
 *       any thread is spawned.
 *   <li>{@code SqlTableFragment.cachedSchema} lazy-init is a plain-field data race — benign
 *       (both threads compute the same value) but not formally safe. Init in {@code @BeforeAll}
 *       avoids the race in practice.
 *   <li>Do not call {@code clearInspect()} while threads are running — it races with concurrent
 *       {@code process()} appends.
 * </ul>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcurrentTransactionsIT {

    static final int THREAD_COUNT = 20;
    static final String TX_ID = "concurrent_select_orders";
    static final String TX_SQL = "SELECT * FROM orders";

    static DataSource monolithDs;
    static Monolith monolith;
    static Partitioner partitioner;
    static DefaultProxy proxy;
    static AggregatorService aggregatorService;
    static Result stubResult;

    @BeforeAll
    static void setUpAll() throws SQLException {
        // Distinct DB name to avoid collision with ReadDecouplingIT / WriteDecouplingIT
        monolithDs = ECommerceSchemaSetup.createMonolithDataSource("monolith_concurrent");
        ECommerceSchemaSetup.setup(monolithDs);

        monolith = DecouplerFactory.createMonolith(monolithDs);
        partitioner = DecouplerFactory.createPartitioner(monolithDs);
        aggregatorService = DecouplerFactory.createAggregatorService();
        proxy = DecouplerFactory.createProxy(partitioner);
        // Single-threaded init — triggers SqlPartitioner lazy-init and Fragment::init()
        proxy.init(monolith, DecouplerFactory.createDecomposition(aggregatorService));

        stubResult = SqlResult.ofRows(List.of(Map.of("id", 99)));
        Aggregator stub = stubAggregator(TX_ID, stubResult);
        aggregatorService.put(stub);
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        ECommerceSchemaSetup.teardown(monolithDs);
    }

    @Test
    @Order(1)
    @DisplayName("MONOLITH phase — 20 concurrent calls all return results")
    void concurrent_monolith_phase_all_results_returned() throws Exception {
        Transaction tx = SqlTransaction.of(TX_ID, TX_SQL);
        proxy.clearInspect();

        List<Result> results = runConcurrently(THREAD_COUNT, () -> proxy.process(tx));

        assertThat(results).hasSize(THREAD_COUNT).doesNotContainNull();
        assertThat(proxy.inspect())
                .hasSize(THREAD_COUNT)
                .allMatch(e -> e.contains("MONOLITH"));
    }

    @Test
    @Order(2)
    @DisplayName("SHADOW phase — 20 concurrent calls log all entries, zero mismatches")
    void concurrent_shadow_phase_log_records_all_calls() throws Exception {
        Transaction tx = SqlTransaction.of(TX_ID, TX_SQL);
        proxy.promote(tx); // MONOLITH → SHADOW
        proxy.clearInspect();

        List<Result> results = runConcurrently(THREAD_COUNT, () -> proxy.process(tx));

        assertThat(results).hasSize(THREAD_COUNT).doesNotContainNull();
        assertThat(proxy.inspect())
                .hasSize(THREAD_COUNT)
                .allMatch(e -> e.contains("SHADOW[" + TX_ID));
    }

    @Test
    @Order(3)
    @DisplayName("DECOUPLED phase — 20 concurrent calls all return stub result")
    void concurrent_decoupled_phase_stub_aggregator() throws Exception {
        Transaction tx = SqlTransaction.of(TX_ID, TX_SQL);
        proxy.promote(tx); // SHADOW → DECOUPLED
        proxy.clearInspect();

        List<Result> results = runConcurrently(THREAD_COUNT, () -> proxy.process(tx));

        assertThat(results).hasSize(THREAD_COUNT).allSatisfy(r ->
                assertThat(r.message()).isEqualTo(stubResult.message()));
        assertThat(proxy.inspect())
                .hasSize(THREAD_COUNT)
                .allMatch(e -> e.contains("DECOUPLED"));
    }

    @Test
    @Order(4)
    @DisplayName("Mixed transaction IDs — concurrent routing stays isolated per ID")
    void concurrent_mixed_transaction_ids() throws Exception {
        String idA = "tx_concurrent_a";
        String idB = "tx_concurrent_b";
        Result resultA = SqlResult.ofRows(List.of(Map.of("id", "a")));
        Result resultB = SqlResult.ofRows(List.of(Map.of("id", "b")));
        aggregatorService.put(stubAggregator(idA, resultA));
        aggregatorService.put(stubAggregator(idB, resultB));

        Transaction txA = SqlTransaction.of(idA, TX_SQL);
        Transaction txB = SqlTransaction.of(idB, TX_SQL);
        proxy.promote(txA); proxy.promote(txA); // → DECOUPLED
        proxy.promote(txB); proxy.promote(txB); // → DECOUPLED
        proxy.clearInspect();

        int half = THREAD_COUNT / 2;
        List<Callable<Result>> callables = new ArrayList<>();
        for (int i = 0; i < half; i++) callables.add(() -> proxy.process(txA));
        for (int i = 0; i < half; i++) callables.add(() -> proxy.process(txB));

        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        List<Future<Result>> futures = callables.stream()
                .map(c -> pool.submit(() -> { barrier.await(); return c.call(); }))
                .toList();
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) results.add(f.get());

        long countA = results.stream().filter(r -> r.message().equals(resultA.message())).count();
        long countB = results.stream().filter(r -> r.message().equals(resultB.message())).count();
        assertThat(countA).isEqualTo(half);
        assertThat(countB).isEqualTo(half);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private List<Result> runConcurrently(int n, Callable<Result> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(n);
        CyclicBarrier barrier = new CyclicBarrier(n);
        List<Future<Result>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            futures.add(pool.submit(() -> {
                barrier.await();
                return task.call();
            }));
        }
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.SECONDS);

        List<Result> results = new ArrayList<>();
        for (Future<Result> f : futures) results.add(f.get());
        return results;
    }

    static Aggregator stubAggregator(String txId, Result fixedResult) {
        return new Aggregator() {
            @Override public String getTxSig() { return txId; }
            @Override public boolean canHandle(Transaction tx) { return txId.equals(tx.id()); }
            @Override public Result execute(Transaction tx, Partition partition) { return fixedResult; }
        };
    }
}
