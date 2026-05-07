package com.decoupler.interfaces;

import java.util.Map;

/**
 * A single interceptable unit of work submitted to the system.
 *
 * <p>{@code id} is the stable, operator-assigned routing key.
 * {@code logic} is the implementation-specific payload (e.g. a SQL string, an HTTP method+path).
 * {@code params} are the named runtime arguments bound to the logic.
 */
public interface Transaction {
    String id();
    String logic();
    Map<String, Object> params();
    default Map<String, String> metadata() { return Map.of(); }
}
