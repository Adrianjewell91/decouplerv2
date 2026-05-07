package com.decoupler.integration;

import com.decoupler.impl.DefaultProxy;
import com.decoupler.impl.decomposition.PlanBasedAggregatorBuilder;
import com.decoupler.impl.sql.SqlReadCommand;
import com.decoupler.impl.sql.SqlTransaction;
import com.decoupler.interfaces.*;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test: WRITE transaction decoupling — dual-write (SHADOW) phase.
 *
 * <p>Requires Ollama running locally with {@code qwen3-coder:30b}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WriteDecouplingIT {

    private static final Logger log = LoggerFactory.getLogger(WriteDecouplingIT.class);

    static final String QUERY_ID  = "insert_order_v2";
    static final String QUERY_SQL = "INSERT INTO orders (customer_id, total_amount, status) VALUES (?, ?, ?)";

    static final int    NEW_CUSTOMER_ID  = 2;
    static final double NEW_TOTAL_AMOUNT = 299.99;
    static final String NEW_STATUS       = "PENDING";

    static DataSource monolithDs;
    static Monolith monolith;
    static Partitioner wrappersPartitioner;
    static DefaultProxy proxy;
    static AggregatorService aggregatorService;
    static Fragment ordersFragment;

    @BeforeAll
    static void setUpAll() throws SQLException {
        monolithDs = ECommerceSchemaSetup.createMonolithDataSource();
        ECommerceSchemaSetup.setup(monolithDs);

        monolith = DecouplerFactory.createMonolith(monolithDs);
        wrappersPartitioner = DecouplerFactory.createPartitioner(monolithDs);
        aggregatorService = DecouplerFactory.createAggregatorService();
        proxy = DecouplerFactory.createProxy(wrappersPartitioner);
        proxy.init(monolith, DecouplerFactory.createDecomposition(aggregatorService));

        // Get ordersFragment from the proxy's already-initialised partition
        ordersFragment = proxy.getCachedPartition().fragments().stream()
                .filter(f -> "orders".equals(f.schema().name()))
                .findFirst()
                .orElseThrow();

        log.info("=== WriteDecouplingIT setup complete ===");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        ECommerceSchemaSetup.teardown(monolithDs);
    }

    @Test
    @Order(1)
    @DisplayName("Step 1: MONOLITH — write goes only to SQL monolith")
    void step1_monolithOnlyWrite() throws Exception {
        int monolithBefore = countOrders(monolithDs);
        int fragmentBefore = countFragmentOrders();

        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL,
                NEW_CUSTOMER_ID, NEW_TOTAL_AMOUNT, NEW_STATUS);

        Result result = proxy.process(tx);
        assertThat(result.message()).isEqualTo(1);

        assertThat(countOrders(monolithDs)).isEqualTo(monolithBefore + 1);
        assertThat(countFragmentOrders()).isEqualTo(fragmentBefore);

        log.info("Step 1 passed: monolith={} orders; fragment={} orders",
                countOrders(monolithDs), countFragmentOrders());
    }

    @Test
    @Order(2)
    @DisplayName("Step 2: Deploy — LLM generates INSERT plan and registers aggregator")
    void step2_deploy() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL,
                NEW_CUSTOMER_ID, NEW_TOTAL_AMOUNT, NEW_STATUS);
        Partition partition = monolith.partition(wrappersPartitioner);

        Aggregator.Builder builder = new PlanBasedAggregatorBuilder()
                .transaction(tx)
                .partition(partition)
                .llmClient(DecouplerFactory.createLlmClient())
                .timeout(Duration.ofSeconds(300));

        Aggregator aggregator = DecouplerFactory.createDecomposition(aggregatorService).build(builder);
        DecouplerFactory.createDecomposition(aggregatorService).register(aggregator, aggregatorService);

        assertThat(aggregator).isNotNull();
        assertThat(aggregator.getTxSig()).isEqualTo(QUERY_ID);
        log.info("Step 2 passed: deployed write aggregator [{}]", aggregator.getTxSig());
    }

    @Test
    @Order(3)
    @DisplayName("Step 3: SHADOW — insert reaches BOTH monolith and fragment")
    void step3_shadowWrite() throws Exception {
        Transaction tx = SqlTransaction.of(QUERY_ID, QUERY_SQL,
                NEW_CUSTOMER_ID, NEW_TOTAL_AMOUNT + 50.0, "CONFIRMED");

        proxy.promote(tx);

        int monolithBefore = countOrders(monolithDs);
        int fragmentBefore = countFragmentOrders();

        Result result = proxy.process(tx);
        assertThat(result.message()).isEqualTo(1);

        assertThat(countOrders(monolithDs))
                .as("Monolith order count should have incremented")
                .isEqualTo(monolithBefore + 1);
        assertThat(countFragmentOrders())
                .as("Fragment should also have received the insert")
                .isEqualTo(fragmentBefore + 1);

        log.info("Step 3 passed: monolith={} orders, fragment={} orders",
                countOrders(monolithDs), countFragmentOrders());
    }

    @Test
    @Order(4)
    @DisplayName("Step 4: Verify fragment received the correct data")
    void step4_verifyFragmentContent() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> confirmedOrders = (List<Map<String, Object>>)
                ordersFragment.execute(new SqlReadCommand(Map.of("status", "CONFIRMED")))
                        .message();

        assertThat(confirmedOrders)
                .as("Fragment should contain the CONFIRMED order inserted in step 3")
                .isNotEmpty();

        Map<String, Object> order = confirmedOrders.get(confirmedOrders.size() - 1);
        assertThat(order.get("status")).isEqualTo("CONFIRMED");
        assertThat(((Number) order.get("customer_id")).intValue()).isEqualTo(NEW_CUSTOMER_ID);

        log.info("Step 4 passed: fragment order = {}", order);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private int countOrders(DataSource ds) {
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM orders");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int countFragmentOrders() {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>)
                ordersFragment.execute(new SqlReadCommand(Map.of())).message();
        return rows.size();
    }
}
