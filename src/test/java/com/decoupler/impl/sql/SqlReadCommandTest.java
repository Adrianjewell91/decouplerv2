package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlReadCommandTest {

    static DataSource ds;
    static SqlTableFragment fragment;

    @BeforeAll
    static void setUpAll() throws SQLException {
        ds = H2TestSupport.createDataSource("read_cmd_test");
        H2TestSupport.createItemsTable(ds);
        H2TestSupport.insertRow(ds, "alpha", 1);
        H2TestSupport.insertRow(ds, "beta", 2);
        fragment = new SqlTableFragment(ds, "items");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        H2TestSupport.dropItemsTable(ds);
    }

    @Test
    void describeContainsRead() {
        assertThat(new SqlReadCommand(Map.of()).describe()).containsIgnoringCase("READ");
    }

    @Test
    void buildProducesSelectSql() {
        String sql = new SqlReadCommand(Map.of()).build(fragment.schema());
        assertThat(sql).startsWith("SELECT * FROM items");
    }

    @Test
    void buildWithFilterAddsWhereClause() {
        String sql = new SqlReadCommand(Map.of("name", "alpha")).build(fragment.schema());
        assertThat(sql).contains("WHERE").contains("name = ?");
    }

    @Test
    void paramsReturnsFilterValues() {
        Map<String, Object> params = new SqlReadCommand(Map.of("name", "alpha")).params();
        assertThat(params).containsEntry("name", "alpha");
    }

    @Test
    void executeWithEmptyFilterReturnsAllRows() {
        Result r = fragment.execute(new SqlReadCommand(Map.of()));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) r.message();
        assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void executeWithFilterReturnsMatchingRowsOnly() {
        Result r = fragment.execute(new SqlReadCommand(Map.of("name", "alpha")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) r.message();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("name")).isEqualTo("alpha");
    }

    @Test
    void executeWithNonMatchingFilterReturnsEmpty() {
        Result r = fragment.execute(new SqlReadCommand(Map.of("name", "nonexistent")));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) r.message();
        assertThat(rows).isEmpty();
    }
}
