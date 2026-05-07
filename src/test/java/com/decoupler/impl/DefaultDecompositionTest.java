package com.decoupler.impl;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultDecompositionTest {

    @Mock AggregatorService service;
    @Mock Aggregator aggregator;
    @Mock Aggregator.Builder builder;
    @Mock Transaction tx;
    @Mock Partition partition;
    @Mock Result result;

    private DefaultDecomposition decomposition() {
        return new DefaultDecomposition(service);
    }

    @Test
    void buildDelegatesToBuilderBuild() throws BuildException {
        when(builder.build()).thenReturn(aggregator);

        Aggregator built = decomposition().build(builder);

        assertThat(built).isSameAs(aggregator);
        verify(builder).build();
    }

    @Test
    void registerCallsServicePut() {
        decomposition().register(aggregator, service);

        verify(service).put(aggregator);
    }

    @Test
    void executeCallsAggregatorExecuteAfterFindingViaService() throws ExecutionException {
        when(service.get(tx)).thenReturn(aggregator);
        when(aggregator.execute(tx, partition)).thenReturn(result);

        Result actual = decomposition().execute(tx, partition);

        assertThat(actual).isSameAs(result);
        verify(aggregator).execute(tx, partition);
    }

    @Test
    void executeThrowsExecutionExceptionWhenNoAggregatorFound() {
        when(service.get(tx)).thenReturn(null);
        when(tx.id()).thenReturn("missingTx");

        assertThatThrownBy(() -> decomposition().execute(tx, partition))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("missingTx");
    }
}
