package com.decoupler.integration;

import com.decoupler.impl.sql.H2TestSupport;
import com.decoupler.impl.sql.SqlReadCommand;
import com.decoupler.impl.sql.SqlTableFragment;
import com.decoupler.impl.sql.JdbcTableReplicator;
import com.decoupler.interfaces.*;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Demonstrates {@link Partition#withFragment} — the only permitted way to extend a partition.
 *
 * <p>Verifies immutability of the original, additive accumulation via chaining, command
 * forwarding, and the known precondition that fragments must be initialised before being
 * added via {@code withFragment()}.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PartitionCompositionIT {

    static DataSource monolithDs;
    static Monolith monolith;
    static com.decoupler.impl.DefaultProxy proxy;
    static Partition basePartition;

    @BeforeAll
    static void setUpAll() throws SQLException {
        monolithDs = ECommerceSchemaSetup.createMonolithDataSource("monolith_composition");
        ECommerceSchemaSetup.setup(monolithDs);

        monolith = DecouplerFactory.createMonolith(monolithDs);
        Partitioner partitioner = DecouplerFactory.createPartitioner(monolithDs);
        AggregatorService svc = DecouplerFactory.createAggregatorService();
        proxy = DecouplerFactory.createProxy(partitioner);
        proxy.init(monolith, DecouplerFactory.createDecomposition(svc));

        basePartition = proxy.getCachedPartition();
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        ECommerceSchemaSetup.teardown(monolithDs);
    }

    @Test
    @Order(1)
    @DisplayName("withFragment adds one fragment without mutating the original")
    void withFragment_adds_fragment_without_mutating_original() throws Exception {
        Fragment extra = directFragment("extra_a");
        int originalSize = basePartition.fragments().size();

        Partition extended = basePartition.withFragment(extra);

        assertThat(extended.fragments()).hasSize(originalSize + 1);
        assertThat(basePartition.fragments()).hasSize(originalSize);
    }

    @Test
    @Order(2)
    @DisplayName("Chained withFragment calls accumulate independently")
    void withFragment_chained_calls_accumulate() throws Exception {
        Fragment extra1 = directFragment("extra_b1");
        Fragment extra2 = directFragment("extra_b2");
        int originalSize = basePartition.fragments().size();

        Partition first  = basePartition.withFragment(extra1);
        Partition second = first.withFragment(extra2);

        assertThat(first.fragments()).hasSize(originalSize + 1);
        assertThat(second.fragments()).hasSize(originalSize + 2);
        assertThat(basePartition.fragments()).hasSize(originalSize);
    }

    @Test
    @Order(3)
    @DisplayName("Commands are forwarded from the original partition")
    void withFragment_commands_are_forwarded_from_original() throws Exception {
        Fragment extra = directFragment("extra_c");

        Partition extended = basePartition.withFragment(extra);

        assertThat(extended.commands()).isSameAs(basePartition.commands());
    }

    @Test
    @Order(4)
    @DisplayName("New fragment is executable after withFragment when init() was called first")
    void withFragment_new_fragment_is_executable() throws Exception {
        DataSource ds = H2TestSupport.createDataSource("composition_exec");
        H2TestSupport.createItemsTable(ds);
        H2TestSupport.insertRow(ds, "widget", 5);

        // Direct-mode fragment — init() is a no-op but the DataSource is already live
        Fragment items = new SqlTableFragment(ds, "items");
        items.init();

        Partition extended = basePartition.withFragment(items);
        Fragment found = extended.fragments().stream()
                .filter(f -> "items".equals(f.schema().name()))
                .findFirst()
                .orElseThrow();

        Result result = found.execute(new SqlReadCommand(Map.of()));
        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSize(1);
    }

    @Test
    @Order(5)
    @DisplayName("Uninitialised replicated fragment throws when executed after withFragment")
    void withFragment_uninitialised_replicated_fragment_throws_on_execute() {
        // Replicated mode, init() deliberately NOT called
        // withFragment() itself must succeed — it does not call init()
        Fragment uninitialised = new SqlTableFragment("orders",
                new JdbcTableReplicator(monolithDs),
                H2TestSupport::createDataSource);

        Partition extended = basePartition.withFragment(uninitialised);

        Fragment found = extended.fragments().stream()
                .filter(f -> f == uninitialised)
                .findFirst()
                .orElseThrow();

        // execute() on an uninitialised replicated fragment NPEs on ownDataSource
        assertThrows(Exception.class, () -> found.execute(new SqlReadCommand(Map.of())));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static Fragment directFragment(String dbName) throws SQLException {
        DataSource ds = H2TestSupport.createDataSource(dbName);
        H2TestSupport.createItemsTable(ds);
        SqlTableFragment f = new SqlTableFragment(ds, "items");
        f.init();
        return f;
    }
}
