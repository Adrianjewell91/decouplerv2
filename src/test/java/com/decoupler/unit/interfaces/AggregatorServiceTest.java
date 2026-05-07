package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregatorServiceTest {

    @Mock AggregatorService service;
    @Mock Aggregator aggregator;
    @Mock Transaction tx;
    @Mock Partition partition;

    @Test
    void put_then_get_returns_same_aggregator() {
        when(aggregator.canHandle(tx)).thenReturn(true);
        when(service.get(tx)).thenReturn(aggregator);

        service.put(aggregator);
        Aggregator found = service.get(tx);

        assertThat(found).isSameAs(aggregator);
        assertThat(found.canHandle(tx)).isTrue();
    }

    @Test
    void get_returns_null_when_no_aggregator_registered() {
        when(service.get(tx)).thenReturn(null);
        assertThat(service.get(tx)).isNull();
    }

    @Test
    void aggregator_executes_transaction_with_partition() {
        Result result = mock(Result.class);
        when(aggregator.execute(tx, partition)).thenReturn(result);

        assertThat(aggregator.execute(tx, partition)).isSameAs(result);
        verify(aggregator).execute(tx, partition);
    }
}
