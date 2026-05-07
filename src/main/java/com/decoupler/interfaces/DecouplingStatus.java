package com.decoupler.interfaces;

/**
 * Lifecycle state of a transaction's migration from monolith to decomposition.
 *
 * <pre>
 *   MONOLITH ──► SHADOW ──► DECOUPLED
 * </pre>
 *
 * <p>SHADOW covers both read-shadowing (compare results) and write-shadowing
 * (dual-write); the distinction is an implementation detail of the {@link Proxy}.
 */
public enum DecouplingStatus {
    /** All traffic routed to the monolith. Initial state. */
    MONOLITH,
    /** Traffic sent to both monolith and decomposition; monolith is authoritative. */
    SHADOW,
    /** All traffic routed to the decomposition layer; monolith no longer involved. */
    DECOUPLED
}
