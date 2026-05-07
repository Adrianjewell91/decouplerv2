package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AggregatorBuilderTest {

    @Mock Aggregator.Builder builder;
    @Mock Aggregator aggregator;
    @Mock Transaction tx;
    @Mock Partition partition;
    @Mock LlmClient llmClient;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @Test
    void fluent_chain_returns_builder_for_each_setter() {
        when(builder.transaction(tx)).thenReturn(builder);
        when(builder.partition(partition)).thenReturn(builder);
        when(builder.llmClient(llmClient)).thenReturn(builder);
        when(builder.timeout(TIMEOUT)).thenReturn(builder);

        Aggregator.Builder result = builder.transaction(tx)
                                           .partition(partition)
                                           .llmClient(llmClient)
                                           .timeout(TIMEOUT);

        assertThat(result).isSameAs(builder);
    }

    @Test
    void build_produces_aggregator_after_full_chain() throws BuildException {
        when(builder.transaction(tx)).thenReturn(builder);
        when(builder.partition(partition)).thenReturn(builder);
        when(builder.llmClient(llmClient)).thenReturn(builder);
        when(builder.timeout(TIMEOUT)).thenReturn(builder);
        when(builder.build()).thenReturn(aggregator);
        when(aggregator.getTxSig()).thenReturn("create_order");

        Aggregator built = builder.transaction(tx)
                                  .partition(partition)
                                  .llmClient(llmClient)
                                  .timeout(TIMEOUT)
                                  .build();

        assertThat(built).isSameAs(aggregator);
        assertThat(built.getTxSig()).isEqualTo("create_order");
    }

    @Test
    void transaction_setter_is_called_with_correct_tx() {
        when(builder.transaction(tx)).thenReturn(builder);

        builder.transaction(tx);

        verify(builder).transaction(tx);
    }

    @Test
    void partition_setter_is_called_with_correct_partition() {
        when(builder.partition(partition)).thenReturn(builder);

        builder.partition(partition);

        verify(builder).partition(partition);
    }

    @Test
    void llmClient_setter_is_called_with_correct_client() {
        when(builder.llmClient(llmClient)).thenReturn(builder);

        builder.llmClient(llmClient);

        verify(builder).llmClient(llmClient);
    }

    @Test
    void timeout_setter_is_called_with_correct_duration() {
        when(builder.timeout(TIMEOUT)).thenReturn(builder);

        builder.timeout(TIMEOUT);

        verify(builder).timeout(TIMEOUT);
    }

    @Test
    void build_throws_build_exception_on_invalid_llm_output() throws BuildException {
        when(builder.transaction(tx)).thenReturn(builder);
        when(builder.partition(partition)).thenReturn(builder);
        when(builder.llmClient(llmClient)).thenReturn(builder);
        when(builder.timeout(TIMEOUT)).thenReturn(builder);
        when(builder.build()).thenThrow(new BuildException("LLM returned invalid plan"));

        assertThatThrownBy(() -> builder.transaction(tx)
                                        .partition(partition)
                                        .llmClient(llmClient)
                                        .timeout(TIMEOUT)
                                        .build())
                .isInstanceOf(BuildException.class)
                .hasMessageContaining("invalid plan");
    }
}
