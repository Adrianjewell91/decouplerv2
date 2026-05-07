package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlResultTest {

    @Test
    void ofRowsMessageReturnsTheList() {
        List<Map<String, Object>> rows = List.of(Map.of("id", 1));
        Result r = SqlResult.ofRows(rows);
        assertThat(r.message()).isEqualTo(rows);
    }

    @Test
    void ofCountMessageReturnsTheCount() {
        Result r = SqlResult.ofCount(42);
        assertThat(r.message()).isEqualTo(42);
    }

    @Test
    void twoOfRowsWithSameListAreEqual() {
        List<Map<String, Object>> rows = List.of(Map.of("id", 1));
        Result r1 = SqlResult.ofRows(rows);
        Result r2 = SqlResult.ofRows(rows);
        assertThat(r1).isEqualTo(r2);
        assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
    }

    @Test
    void ofCountAndOfRowsAreNotEqual() {
        Result r1 = SqlResult.ofCount(1);
        Result r2 = SqlResult.ofRows(List.of(Map.of("id", 1)));
        assertThat(r1).isNotEqualTo(r2);
    }
}
