package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.DecouplingStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DecouplingStatus: enum contract and lifecycle ordering.
 */
class DecouplingStatusTest {

    @Test
    void three_states_exist() {
        assertThat(DecouplingStatus.values())
                .containsExactly(DecouplingStatus.MONOLITH, DecouplingStatus.SHADOW, DecouplingStatus.DECOUPLED);
    }

    @Test
    void lifecycle_order_is_monolith_shadow_decoupled() {
        DecouplingStatus[] values = DecouplingStatus.values();
        assertThat(values[0]).isEqualTo(DecouplingStatus.MONOLITH);
        assertThat(values[1]).isEqualTo(DecouplingStatus.SHADOW);
        assertThat(values[2]).isEqualTo(DecouplingStatus.DECOUPLED);
    }

    @Test
    void monolith_is_initial_state() {
        assertThat(DecouplingStatus.MONOLITH.ordinal()).isZero();
    }

    @Test
    void decoupled_is_terminal_state() {
        assertThat(DecouplingStatus.DECOUPLED.ordinal())
                .isEqualTo(DecouplingStatus.values().length - 1);
    }
}
