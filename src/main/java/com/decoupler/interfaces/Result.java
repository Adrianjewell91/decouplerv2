package com.decoupler.interfaces;

/**
 * Opaque result of executing a {@link Transaction} or a {@link Command}.
 *
 * <p>The structure of {@code message()} is determined by the implementation.
 * The framework never inspects it — comparison and interpretation are done
 * by implementation-level components (e.g. the Proxy's result comparator).
 */
public interface Result {
    Object message();
}
