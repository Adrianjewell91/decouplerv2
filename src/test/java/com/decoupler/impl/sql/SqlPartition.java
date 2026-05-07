package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Fragment;
import com.decoupler.interfaces.Partition;

import java.util.List;

/**
 * Immutable {@link Partition} holding SQL table fragments and their available commands.
 *
 * <p>Both lists are defensively copied on construction via {@link List#copyOf}, so callers
 * cannot modify the underlying collections through the references they passed in.
 */
public record SqlPartition(List<Fragment> fragments, List<Command> commands) implements Partition {

    public SqlPartition(List<Fragment> fragments, List<Command> commands) {
        this.fragments = List.copyOf(fragments);
        this.commands = List.copyOf(commands);
    }
}
