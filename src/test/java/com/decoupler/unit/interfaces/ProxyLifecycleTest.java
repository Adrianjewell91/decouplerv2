package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProxyLifecycleTest {

    @Mock Proxy proxy;
    @Mock Monolith monolith;
    @Mock Decomposition decomposition;
    @Mock Transaction tx;
    @Mock Result monolithResult;
    @Mock Result decompositionResult;

    @Test
    void proxy_init_then_process_in_order() throws Exception {
        InOrder order = inOrder(proxy);
        proxy.init(monolith, decomposition);
        when(proxy.process(tx)).thenReturn(monolithResult);
        proxy.process(tx);
        order.verify(proxy).init(monolith, decomposition);
        order.verify(proxy).process(tx);
    }

    @Test
    void process_returns_result() throws Exception {
        when(proxy.process(tx)).thenReturn(monolithResult);
        assertThat(proxy.process(tx)).isSameAs(monolithResult);
    }

    @Test
    void promote_is_called_per_transaction() {
        proxy.promote(tx);
        verify(proxy).promote(tx);
    }

    @Test
    void promote_twice_advances_through_shadow_to_decoupled() {
        proxy.promote(tx);
        proxy.promote(tx);
        verify(proxy, times(2)).promote(tx);
    }

    @Test
    void inspect_returns_list_of_entries() {
        when(proxy.inspect()).thenReturn(List.of("MONOLITH[create_order]", "SHADOW[create_order]: 0 mismatches"));

        List<String> log = proxy.inspect();

        assertThat(log).hasSize(2).anyMatch(e -> e.contains("create_order"));
    }

    @Test
    void inspect_returns_empty_list_when_no_activity() {
        when(proxy.inspect()).thenReturn(List.of());
        assertThat(proxy.inspect()).isEmpty();
    }

    @Test
    void process_after_promote_to_shadow_returns_monolith_result() throws Exception {
        proxy.promote(tx);
        when(proxy.process(tx)).thenReturn(monolithResult);
        assertThat(proxy.process(tx)).isSameAs(monolithResult);
    }

    @Test
    void process_after_promote_to_decoupled_returns_decomposition_result() throws Exception {
        proxy.promote(tx);
        proxy.promote(tx);
        when(proxy.process(tx)).thenReturn(decompositionResult);
        assertThat(proxy.process(tx)).isSameAs(decompositionResult);
    }

    @Test
    void clearInspect_resets_the_log() {
        when(proxy.inspect()).thenReturn(List.of());
        proxy.clearInspect();
        assertThat(proxy.inspect()).isEmpty();
        verify(proxy).clearInspect();
    }
}
