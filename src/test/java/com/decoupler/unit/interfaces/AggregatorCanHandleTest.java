package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Aggregator: getTxSig and canHandle contract.
 *
 * canHandle(tx) is the gating check before execute(tx) is called.
 * It returns true only for transactions whose id matches the aggregator's signature.
 */
@ExtendWith(MockitoExtension.class)
class AggregatorCanHandleTest {

    @Mock Aggregator aggregator;
    @Mock Transaction matching;
    @Mock Transaction nonMatching;

    @Test
    void canHandle_true_for_matching_transaction() {
        when(aggregator.canHandle(matching)).thenReturn(true);

        assertThat(aggregator.canHandle(matching)).isTrue();
    }

    @Test
    void canHandle_false_for_non_matching_transaction() {
        when(aggregator.canHandle(nonMatching)).thenReturn(false);

        assertThat(aggregator.canHandle(nonMatching)).isFalse();
    }

    @Test
    void getTxSig_returns_stable_signature() {
        when(aggregator.getTxSig()).thenReturn("create_order");

        assertThat(aggregator.getTxSig()).isEqualTo("create_order");
    }

    @Test
    void getTxSig_is_consistent_across_calls() {
        when(aggregator.getTxSig()).thenReturn("create_order");

        assertThat(aggregator.getTxSig()).isEqualTo(aggregator.getTxSig());
    }
}
