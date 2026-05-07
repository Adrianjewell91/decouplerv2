package com.decoupler.interfaces;

/**
 * Abstraction over the legacy monolith.
 *
 * <p>Executes transactions directly and can partition itself into {@link Fragment}s
 * and {@link Command}s for use by the decomposition layer.
 *
 * <p>{@code init()} verifies that the underlying data source is reachable before
 * any traffic is routed through it. Callers should invoke {@code init()} once
 * before calling {@code execute()} or {@code partition()}.
 *
 * <p>Once {@code partition()} has been called and a {@link Partition} is in use,
 * the monolith schema is expected to be stable. Implementations should treat the
 * schema as immutable for the lifetime of the partition.
 */
public interface Monolith {
    void init();
    void close();
    Result execute(Transaction tx);
    Partition partition(Partitioner partitioner);
}
