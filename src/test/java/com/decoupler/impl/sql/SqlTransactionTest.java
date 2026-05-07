package com.decoupler.impl.sql;

import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class SqlTransactionTest {

    @Test
    void accessorsReturnCorrectValues() {
        Transaction tx = SqlTransaction.of("myId", "SELECT 1", "a", "b");

        assertThat(tx.id()).isEqualTo("myId");
        assertThat(tx.logic()).isEqualTo("SELECT 1");
        assertThat(tx.params()).containsEntry("0", "a").containsEntry("1", "b");
    }

    @Test
    void paramsMapPreservesInsertionOrder() {
        Transaction tx = SqlTransaction.of("id", "SELECT 1", 10, 20, 30);
        List<String> keys = List.copyOf(tx.params().keySet());
        assertThat(keys).containsExactly("0", "1", "2");
    }

    @Test
    void paramsMapHasCorrectValues() {
        Transaction tx = SqlTransaction.of("id", "SELECT 1", "alpha", "beta");
        Map<String, Object> p = tx.params();
        assertThat(p.get("0")).isEqualTo("alpha");
        assertThat(p.get("1")).isEqualTo("beta");
    }

    @Test
    void emptyParamsProducesEmptyMap() {
        Transaction tx = SqlTransaction.of("id", "SELECT 1");
        assertThat(tx.params()).isEmpty();
    }
}
