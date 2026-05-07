package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlMonolithTest {

    static DataSource ds;
    static SqlMonolith monolith;

    @BeforeAll
    static void setUpAll() throws SQLException {
        ds = H2TestSupport.createDataSource("monolith_test");
        H2TestSupport.createItemsTable(ds);
        H2TestSupport.insertRow(ds, "alpha", 1);
        H2TestSupport.insertRow(ds, "beta", 2);
        monolith = new SqlMonolith(ds);
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        H2TestSupport.dropItemsTable(ds);
    }

    @Test
    void selectRoutesThroughReadPath() {
        Transaction tx = SqlTransaction.of("read", "SELECT * FROM items");

        Result result = monolith.execute(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void selectWithParamFiltersRows() {
        Transaction tx = SqlTransaction.of("read_filtered", "SELECT * FROM items WHERE name = ?", "alpha");

        Result result = monolith.execute(tx);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result.message();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name")).isEqualTo("alpha");
    }

    @Test
    void insertRoutesThroughWritePath() throws SQLException {
        int before = H2TestSupport.countRows(ds);
        Transaction tx = SqlTransaction.of("write", "INSERT INTO items (name, amt) VALUES (?, ?)", "gamma", 3);

        Result result = monolith.execute(tx);

        assertThat(result.message()).isEqualTo(1);
        assertThat(H2TestSupport.countRows(ds)).isEqualTo(before + 1);
    }

    @Test
    void updateRoutesThroughWritePath() throws SQLException {
        Transaction tx = SqlTransaction.of("update", "UPDATE items SET amt = ? WHERE name = ?", 99, "alpha");

        Result result = monolith.execute(tx);

        assertThat(result.message()).isEqualTo(1);
    }

    @Test
    void deleteRoutesThroughWritePath() throws SQLException {
        H2TestSupport.insertRow(ds, "to_delete", 0);
        int before = H2TestSupport.countRows(ds);
        Transaction tx = SqlTransaction.of("delete", "DELETE FROM items WHERE name = ?", "to_delete");

        Result result = monolith.execute(tx);

        assertThat(result.message()).isEqualTo(1);
        assertThat(H2TestSupport.countRows(ds)).isEqualTo(before - 1);
    }
}
