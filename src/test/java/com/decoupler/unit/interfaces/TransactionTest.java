package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Transaction: id, logic, params contract.
 */
@ExtendWith(MockitoExtension.class)
class TransactionTest {

    @Mock Transaction tx;

    @Test
    void id_identifies_the_transaction() {
        when(tx.id()).thenReturn("create_order");

        assertThat(tx.id()).isEqualTo("create_order");
    }

    @Test
    void logic_carries_the_implementation_payload() {
        when(tx.logic()).thenReturn("INSERT INTO orders (customer_id) VALUES (:customer_id)");

        assertThat(tx.logic()).contains("INSERT INTO orders");
    }

    @Test
    void params_carries_named_arguments() {
        when(tx.params()).thenReturn(Map.of("customer_id", 42, "status", "PENDING"));

        Map<String, Object> params = tx.params();
        assertThat(params).containsEntry("customer_id", 42)
                          .containsEntry("status", "PENDING");
    }

    @Test
    void params_can_be_empty() {
        when(tx.params()).thenReturn(Map.of());

        assertThat(tx.params()).isEmpty();
    }

    @Test
    void metadata_default_returns_empty_map() {
        Transaction plain = new Transaction() {
            @Override public String id() { return "t"; }
            @Override public String logic() { return "SELECT 1"; }
            @Override public Map<String, Object> params() { return Map.of(); }
        };

        assertThat(plain.metadata()).isEmpty();
    }

    @Test
    void metadata_default_map_is_unmodifiable() {
        Transaction plain = new Transaction() {
            @Override public String id() { return "t"; }
            @Override public String logic() { return "SELECT 1"; }
            @Override public Map<String, Object> params() { return Map.of(); }
        };

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> plain.metadata().put("key", "value"));
    }

    @Test
    void metadata_can_be_overridden_by_implementation() {
        Transaction withMeta = new Transaction() {
            @Override public String id() { return "t"; }
            @Override public String logic() { return "SELECT 1"; }
            @Override public Map<String, Object> params() { return Map.of(); }
            @Override public Map<String, String> metadata() { return Map.of("tenant", "acme"); }
        };

        assertThat(withMeta.metadata()).containsEntry("tenant", "acme");
    }
}
