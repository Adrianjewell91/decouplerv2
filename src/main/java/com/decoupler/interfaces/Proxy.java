package com.decoupler.interfaces;

import java.util.List;

/**
 * Single chokepoint for all transaction traffic.
 *
 * <p>Routes each transaction through the decoupling lifecycle:
 * {@code MONOLITH → SHADOW → DECOUPLED}.
 *
 * <p>{@code promote(tx)} advances the transaction's {@link DecouplingStatus}
 * by one step and returns the new status. Calling promote when already
 * {@code DECOUPLED} is a no-op and returns {@code DECOUPLED}.
 *
 * <p>{@code inspect()} returns a human-readable summary of routing decisions
 * and SHADOW-mode comparison mismatches. {@code clearInspect()} resets the log.
 */
public interface Proxy {
    void init(Monolith monolith, Decomposition decomposition);
    Result process(Transaction tx) throws ExecutionException;
    DecouplingStatus promote(Transaction tx);
    List<String> inspect();
    void clearInspect();
}
