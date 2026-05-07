package com.decoupler.impl;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultProxyTest {

    @Mock Monolith monolith;
    @Mock Decomposition decomposition;
    @Mock Partitioner partitioner;
    @Mock Partition partition;
    @Mock Transaction tx;
    @Mock Result monolithResult;
    @Mock Result decompResult;

    DefaultProxy proxy;

    @BeforeEach
    void setUp() {
        when(partitioner.fragments()).thenReturn(List.of());
        when(monolith.partition(partitioner)).thenReturn(partition);
        when(tx.id()).thenReturn("tx1");
        proxy = new DefaultProxy(partitioner);
        proxy.init(monolith, decomposition);
    }

    @Test
    void afterInitProcessRoutesToMonolith() throws ExecutionException {
        when(monolith.execute(tx)).thenReturn(monolithResult);

        Result r = proxy.process(tx);

        assertThat(r).isSameAs(monolithResult);
        verify(monolith).execute(tx);
        verifyNoInteractions(decomposition);
    }

    @Test
    void afterOnePromoteProcessRoutesShadow_returnsMonolithResult() throws ExecutionException {
        proxy.promote(tx);
        when(monolith.execute(tx)).thenReturn(monolithResult);
        when(decomposition.execute(tx, partition)).thenReturn(decompResult);
        when(monolithResult.message()).thenReturn("same");
        when(decompResult.message()).thenReturn("same");

        Result r = proxy.process(tx);

        assertThat(r).isSameAs(monolithResult);
        verify(monolith).execute(tx);
        verify(decomposition).execute(tx, partition);
    }

    @Test
    void afterTwoPromotesProcessRoutesOnlyToDecomposition() throws ExecutionException {
        proxy.promote(tx);
        proxy.promote(tx);
        when(decomposition.execute(tx, partition)).thenReturn(decompResult);

        Result r = proxy.process(tx);

        assertThat(r).isSameAs(decompResult);
        verify(monolith, never()).execute(any());
    }

    @Test
    void inspectIsEmptyInitially() {
        assertThat(proxy.inspect()).isEmpty();
    }

    @Test
    void inspectGrowsAfterProcessCalls() throws ExecutionException {
        when(monolith.execute(tx)).thenReturn(monolithResult);

        proxy.process(tx);

        List<String> log = proxy.inspect();
        assertThat(log).hasSize(1);
        assertThat(log.get(0)).contains("MONOLITH").contains("tx1");
    }

    @Test
    void shadowMismatchRecordedWhenResultsDiffer() throws ExecutionException {
        proxy.promote(tx);
        when(monolith.execute(tx)).thenReturn(monolithResult);
        when(decomposition.execute(tx, partition)).thenReturn(decompResult);
        when(monolithResult.message()).thenReturn("valueA");
        when(decompResult.message()).thenReturn("valueB");

        proxy.process(tx);

        List<String> log = proxy.inspect();
        assertThat(log).hasSize(1);
        assertThat(log.get(0)).contains("MISMATCH");
    }

    @Test
    void promoteReturnsShadowThenDecoupled() {
        DecouplingStatus first = proxy.promote(tx);
        DecouplingStatus second = proxy.promote(tx);
        DecouplingStatus third = proxy.promote(tx);

        assertThat(first).isEqualTo(DecouplingStatus.SHADOW);
        assertThat(second).isEqualTo(DecouplingStatus.DECOUPLED);
        assertThat(third).isEqualTo(DecouplingStatus.DECOUPLED);
    }

    @Test
    void clearInspectEmptiesLog() throws ExecutionException {
        when(monolith.execute(tx)).thenReturn(monolithResult);
        proxy.process(tx);
        assertThat(proxy.inspect()).isNotEmpty();

        proxy.clearInspect();

        assertThat(proxy.inspect()).isEmpty();
    }

}
