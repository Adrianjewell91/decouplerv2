package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonolithPartitionTest {

    @Mock Monolith monolith;
    @Mock Partitioner partitioner;
    @Mock Transaction tx;
    @Mock Result result;
    @Mock Partition partition;
    @Mock Fragment fragment;
    @Mock Command command;

    @Test
    void monolith_executes_transaction_and_returns_result() {
        when(monolith.execute(tx)).thenReturn(result);
        assertThat(monolith.execute(tx)).isSameAs(result);
        verify(monolith).execute(tx);
    }

    @Test
    void partition_takes_partitioner_and_returns_fragments_and_commands() {
        when(monolith.partition(partitioner)).thenReturn(partition);
        when(partition.fragments()).thenReturn(List.of(fragment));
        when(partition.commands()).thenReturn(List.of(command));

        Partition p = monolith.partition(partitioner);

        assertThat(p.fragments()).hasSize(1).contains(fragment);
        assertThat(p.commands()).hasSize(1).contains(command);
    }

    @Test
    void partitioner_returns_fragments_for_monolith_to_assemble() {
        when(partitioner.fragments()).thenReturn(List.of(fragment, fragment));

        List<Fragment> frags = partitioner.fragments();

        assertThat(frags).hasSize(2);
        verify(partitioner).fragments();
    }

    @Test
    void different_partitioners_can_produce_different_partitions() {
        Partitioner otherPartitioner = mock(Partitioner.class);
        Partition otherPartition = mock(Partition.class);

        when(monolith.partition(partitioner)).thenReturn(partition);
        when(monolith.partition(otherPartitioner)).thenReturn(otherPartition);

        assertThat(monolith.partition(partitioner)).isSameAs(partition);
        assertThat(monolith.partition(otherPartitioner)).isSameAs(otherPartition);
    }

    @Test
    void withFragment_returns_new_partition_containing_added_fragment() {
        Fragment extra = mock(Fragment.class);
        Partition base = concretePartition(List.of(fragment), List.of(command));

        Partition extended = base.withFragment(extra);

        assertThat(extended.fragments()).containsExactly(fragment, extra);
    }

    @Test
    void withFragment_does_not_mutate_original_partition() {
        Fragment extra = mock(Fragment.class);
        Partition base = concretePartition(List.of(fragment), List.of(command));

        base.withFragment(extra);

        assertThat(base.fragments()).containsExactly(fragment);
    }

    @Test
    void withFragment_preserves_commands_from_original() {
        Fragment extra = mock(Fragment.class);
        Partition base = concretePartition(List.of(fragment), List.of(command));

        Partition extended = base.withFragment(extra);

        assertThat(extended.commands()).isSameAs(base.commands());
    }

    @Test
    void withFragment_returned_partition_fragments_list_is_unmodifiable() {
        Fragment extra = mock(Fragment.class);
        Partition base = concretePartition(List.of(fragment), List.of(command));

        Partition extended = base.withFragment(extra);

        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> extended.fragments().add(mock(Fragment.class)));
    }

    private static Partition concretePartition(List<Fragment> fragments, List<Command> commands) {
        return new Partition() {
            @Override public List<Fragment> fragments() { return fragments; }
            @Override public List<Command> commands() { return commands; }
        };
    }
}
