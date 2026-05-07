package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlUpdateCommandTest {

    static DataSource ds;
    static SqlTableFragment fragment;

    @BeforeAll
    static void setUpAll() throws SQLException {
        ds = H2TestSupport.createDataSource("update_cmd_test");
        H2TestSupport.createItemsTable(ds);
        H2TestSupport.insertRow(ds, "updateMe", 10);
        H2TestSupport.insertRow(ds, "keepMe", 20);
        fragment = new SqlTableFragment(ds, "items");
    }

    @AfterAll
    static void tearDownAll() throws SQLException {
        H2TestSupport.dropItemsTable(ds);
    }

    @Test
    void describeContainsUpdate() {
        assertThat(new SqlUpdateCommand(Map.of(), Map.of()).describe()).containsIgnoringCase("UPDATE");
    }

    @Test
    void buildProducesUpdateSql() {
        String sql = new SqlUpdateCommand(Map.of("amt", 99), Map.of("name", "x")).build(fragment.schema());
        assertThat(sql).startsWith("UPDATE items SET");
        assertThat(sql).contains("WHERE").contains("name = ?");
    }

    @Test
    void paramsReturnsValuesThenFilter() {
        Map<String, Object> params = new SqlUpdateCommand(Map.of("amt", 99), Map.of("name", "x")).params();
        assertThat(params).containsEntry("amt", 99).containsEntry("name", "x");
        assertThat(params.keySet()).containsExactly("amt", "name");
    }

    @Test
    void executeUpdatesMatchingRows() {
        fragment.execute(new SqlUpdateCommand(Map.of("amt", 99), Map.of("name", "updateMe")));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>)
                fragment.execute(new SqlReadCommand(Map.of("name", "updateMe"))).message();
        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0).get("amt")).intValue()).isEqualTo(99);
    }

    @Test
    void nonMatchingFilterUpdatesNothing() {
        Result r = fragment.execute(new SqlUpdateCommand(Map.of("amt", 0), Map.of("name", "noSuchRow")));
        assertThat(r.message()).isEqualTo(0);
    }
}
