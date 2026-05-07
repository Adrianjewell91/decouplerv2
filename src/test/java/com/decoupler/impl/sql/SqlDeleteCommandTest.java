package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlDeleteCommandTest {

    static DataSource ds;
    static SqlTableFragment fragment;

    @BeforeAll
    static void setUpAll() throws SQLException {
        ds = H2TestSupport.createDataSource("delete_cmd_test");
        H2TestSupport.createItemsTable(ds);
        H2TestSupport.insertRow(ds, "toDelete", 5);
        H2TestSupport.insertRow(ds, "toKeep", 6);
        fragment = new SqlTableFragment(ds, "items");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        H2TestSupport.dropItemsTable(ds);
    }

    @Test
    void describeContainsDelete() {
        assertThat(new SqlDeleteCommand(Map.of()).describe()).containsIgnoringCase("DELETE");
    }

    @Test
    void buildProducesDeleteSql() {
        String sql = new SqlDeleteCommand(Map.of("name", "x")).build(fragment.schema());
        assertThat(sql).startsWith("DELETE FROM items");
        assertThat(sql).contains("WHERE").contains("name = ?");
    }

    @Test
    void paramsReturnsFilterValues() {
        Map<String, Object> params = new SqlDeleteCommand(Map.of("name", "x")).params();
        assertThat(params).containsEntry("name", "x");
    }

    @Test
    void executeDeletesMatchingRows() throws SQLException {
        int before = H2TestSupport.countRows(ds);
        fragment.execute(new SqlDeleteCommand(Map.of("name", "toDelete")));
        assertThat(H2TestSupport.countRows(ds)).isEqualTo(before - 1);
    }

    @Test
    void nonMatchingFilterDeletesNothing() {
        Result r = fragment.execute(new SqlDeleteCommand(Map.of("name", "doesNotExist")));
        assertThat(r.message()).isEqualTo(0);
    }
}
