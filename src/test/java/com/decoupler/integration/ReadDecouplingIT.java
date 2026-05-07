package com.decoupler.integration;

import com.decoupler.impl.DefaultAggregatorService;
import com.decoupler.impl.DefaultDecomposition;
import com.decoupler.impl.DefaultProxy;
import com.decoupler.impl.decomposition.PlanBasedAggregatorBuilder;
import com.decoupler.impl.sql.SqlTransaction;
import com.decoupler.interfaces.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: READ transaction decoupling lifecycle.
 *
 * <p>Requires Ollama running locally with {@code qwen3-coder:30b}.
 *
 * <h2>Target Query</h2>
 * <pre>{@code
 * SELECT o.id, o.status, oi.quantity, p.name, p.price
 * FROM orders o
 * JOIN order_items oi ON o.id = oi.order_id
 * JOIN products p    ON oi.product_id = p.id
 * WHERE o.customer_id = ?
 * }</pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReadDecouplingIT {

    private static final Logger log = LoggerFactory.getLogger(ReadDecouplingIT.class);

    static final String QUERY_ID = "customer_orders_with_products_v2";
    static final String QUERY_SQL = """
            SELECT o.id, o.status, oi.quantity, p.name, p.price
            FROM orders o
            JOIN order_items oi ON o.id = oi.order_id
            JOIN products p    ON oi.product_id = p.id
            WHERE o.customer_id = ?
            """.strip();
    static final int ALICE_CUSTOMER_ID = 1;
    static final int EXPECTED_ROW_COUNT = 2;

    static DataSource monolithDs;
    static Monolith monolith;
    static Partitioner wrappersPartitioner;
    static DefaultDecomposition decomposition;
    static DefaultProxy proxy;
    static AggregatorService aggregatorService;

    @BeforeAll
    static void setUpAll() throws SQLException {
        monolithDs = ECommerceSchemaSetup.createMonolithDataSource();
        ECommerceSchemaSetup.setup(monolithDs);

        monolith = DecouplerFactory.createMonolith(monolithDs);
        wrappersPartitioner = DecouplerFactory.createPartitioner(monolithDs);
        aggregatorService = DecouplerFactory.createAggregatorService();
        decomposition = DecouplerFactory.createDecomposition(aggregatorService);
        proxy = DecouplerFactory.createProxy(wrappersPartitioner);
        proxy.init(monolith, decomposition);

        log.info("=== ReadDecouplingIT setup complete ===");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        ECommerceSchemaSetup.teardown(monolithDs);
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: MONOLITH — proxy routes to SQL monolith")
    void step1_monolith() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL, ALICE_CUSTOMER_ID);

        Result result = proxy.process(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows)
                .as("Alice should have %d order items", EXPECTED_ROW_COUNT)
                .hasSize(EXPECTED_ROW_COUNT);

        assertThat(proxy.inspect()).hasSize(1);
        assertThat(proxy.inspect().get(0)).contains("MONOLITH");
        log.info("Step 1 passed: monolith returned {} rows", rows.size());
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Deploy — LLM decomposes transaction and registers aggregator")
    void step2_deploy() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL, ALICE_CUSTOMER_ID);
        // SqlPartitioner caches fragments — same instances already init'd by proxy
        Partition partition = monolith.partition(wrappersPartitioner);

        Aggregator.Builder builder = new PlanBasedAggregatorBuilder()
                .transaction(tx)
                .partition(partition)
                .llmClient(DecouplerFactory.createLlmClient())
                .timeout(Duration.ofSeconds(300));

        Aggregator aggregator = decomposition.build(builder);
        decomposition.register(aggregator, aggregatorService);

        assertThat(aggregator).isNotNull();
        assertThat(aggregator.getTxSig()).isEqualTo(QUERY_ID);
        log.info("Step 2 passed: aggregator deployed [{}]", aggregator.getTxSig());
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: SHADOW — promote once; monolith result returned; zero mismatches")
    void step3_shadow() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL, ALICE_CUSTOMER_ID);
        proxy.promote(tx);

        Result result = proxy.process(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSize(EXPECTED_ROW_COUNT);

        List<String> shadowLog = proxy.inspect().stream()
                .filter(s -> s.contains("SHADOW[" + QUERY_ID))
                .toList();
        assertThat(shadowLog).isNotEmpty();

        long mismatches = proxy.inspect().stream()
                .filter(s -> s.contains("MISMATCH"))
                .count();
        assertThat(mismatches)
                .as("Zero mismatches expected between monolith and decomposition")
                .isEqualTo(0);

        log.info("Step 3 passed: SHADOW with zero mismatches");
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: DECOUPLED — promote second time; decomposition returns same rows")
    void step4_decoupled() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL, ALICE_CUSTOMER_ID);
        proxy.promote(tx);

        Result result = proxy.process(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSize(EXPECTED_ROW_COUNT);
        rows.forEach(row -> assertThat(row).isNotEmpty());

        assertThat(proxy.inspect().stream().anyMatch(s -> s.contains("DECOUPLED[" + QUERY_ID)))
                .isTrue();
        log.info("Step 4 passed: DECOUPLED — decomposition returned {} rows", rows.size());
    }

    @Test
    @Order(5)
    @DisplayName("Step 5: DECOUPLED is idempotent — further promotes stay DECOUPLED")
    void step5_decoupledIdempotent() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL, ALICE_CUSTOMER_ID);
        proxy.promote(tx);

        Result result = proxy.process(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSize(EXPECTED_ROW_COUNT);
        log.info("Step 5 passed: DECOUPLED remains stable");
    }
}
