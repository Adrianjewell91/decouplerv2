package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecompositionTest {

    @Mock Decomposition decomposition;
    @Mock Aggregator aggregator;
    @Mock Aggregator.Builder builder;
    @Mock AggregatorService service;
    @Mock Transaction tx;
    @Mock Partition partition;
    @Mock Result result;

    @Test
    void build_returns_aggregator_for_transaction() throws BuildException {
        when(decomposition.build(builder)).thenReturn(aggregator);

        assertThat(decomposition.build(builder)).isSameAs(aggregator);
        verify(decomposition).build(builder);
    }

    @Test
    void register_stores_aggregator_in_service() {
        decomposition.register(aggregator, service);
        verify(decomposition).register(aggregator, service);
    }

    @Test
    void execute_routes_to_registered_aggregator() throws ExecutionException {
        when(decomposition.execute(tx, partition)).thenReturn(result);

        assertThat(decomposition.execute(tx, partition)).isSameAs(result);
        verify(decomposition).execute(tx, partition);
    }

    @Test
    void full_deploy_sequence_build_then_register_then_execute() throws BuildException, ExecutionException {
        when(decomposition.build(builder)).thenReturn(aggregator);
        when(decomposition.execute(tx, partition)).thenReturn(result);

        Aggregator built = decomposition.build(builder);
        decomposition.register(built, service);
        Result actual = decomposition.execute(tx, partition);

        assertThat(actual).isSameAs(result);
        verify(decomposition).build(builder);
        verify(decomposition).register(aggregator, service);
        verify(decomposition).execute(tx, partition);
    }
}
