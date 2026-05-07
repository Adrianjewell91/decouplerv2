package com.decoupler.interfaces;

/**
 * A single logical partition of the monolith exposed to the decomposition layer.
 *
 * <p>{@code init()} must be called before {@code execute()} or {@code schema()}.
 * It is where data replication occurs: implementations should open connections,
 * create their own isolated data store, and populate it from the source.
 * {@code init()} must be idempotent — calling it more than once has no effect.
 *
 * <p>{@code execute(command)} calls {@code command.build(schema())} to obtain the SQL,
 * then {@code command.params()} for bind values, and runs both against the fragment's
 * own data store. Command has no reference to Fragment — the dependency is one-way.
 *
 * <p>{@code close()} releases the fragment's resources. Implementing {@link AutoCloseable}
 * enables try-with-resources in lifecycle management code.
 */
public interface Fragment extends AutoCloseable {
    void init();
    void close();
    Result execute(Command command);
    Schema schema();
}
