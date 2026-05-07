package com.decoupler.interfaces;

import java.util.ArrayList;
import java.util.List;

/**
 * An immutable snapshot of a partitioned {@link Monolith}.
 *
 * <p>Groups the data partitions ({@link Fragment}s) and the available
 * operations ({@link Command}s) that can be applied to them. Passed to
 * {@link Decomposition#build} so the aggregator builder has everything it needs.
 *
 * <p><b>Immutability contract:</b> {@code fragments()} and {@code commands()} must
 * return unmodifiable lists. Implementations must not allow callers to modify the
 * underlying collections. Violations break every component that holds a reference
 * to this partition.
 *
 * <p><b>Extension:</b> {@code withFragment(f)} is the only supported way to extend a
 * partition. It always returns a <em>new</em> {@code Partition} that includes the
 * additional {@link Fragment} — the receiver is never mutated. This is how new data
 * sources (e.g. a purpose-built microservice) are composed alongside the monolith
 * fragments when building an {@link Aggregator}.
 */
public interface Partition {
    List<Fragment> fragments();
    List<Command> commands();

    default Partition withFragment(Fragment f) {
        List<Fragment> extended = new ArrayList<>(fragments());
        extended.add(f);
        return new Partition() {
            @Override public List<Fragment> fragments() { return List.copyOf(extended); }
            @Override public List<Command> commands() { return Partition.this.commands(); }
        };
    }
}
