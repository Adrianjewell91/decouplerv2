package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlInsertCommandTest {

    static DataSource ds;
    static SqlTableFragment fragment;

    @BeforeAll
    static void setUpAll() throws SQLException {
        ds = H2TestSupport.createDataSource("insert_cmd_test");
        H2TestSupport.createItemsTable(ds);
        fragment = new SqlTableFragment(ds, "items");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        H2TestSupport.dropItemsTable(ds);
    }

    @Test
    void describeContainsInsert() {
        assertThat(new SqlInsertCommand(Map.of()).describe()).containsIgnoringCase("INSERT");
    }

    @Test
    void buildProducesInsertSql() {
        String sql = new SqlInsertCommand(Map.of("name", "x", "amt", 1)).build(fragment.schema());
        assertThat(sql).startsWith("INSERT INTO items");
        assertThat(sql).contains("name").contains("amt");
    }

    @Test
    void paramsReturnsValues() {
        Map<String, Object> params = new SqlInsertCommand(Map.of("name", "gamma", "amt", 3)).params();
        assertThat(params).containsEntry("name", "gamma").containsEntry("amt", 3);
    }

    @Test
    void executeInsertsRow() throws SQLException {
        int before = H2TestSupport.countRows(ds);
        fragment.execute(new SqlInsertCommand(Map.of("name", "gamma", "amt", 3)));
        assertThat(H2TestSupport.countRows(ds)).isEqualTo(before + 1);
    }

    @Test
    void executeReturnsCountOfOne() {
        Result r = fragment.execute(new SqlInsertCommand(Map.of("name", "delta", "amt", 4)));
        assertThat(r.message()).isEqualTo(1);
    }
}
