package com.decoupler.interfaces;

/**
 * Thrown by {@link Decomposition#execute} when a transaction cannot be executed
 * against the decomposition layer (e.g. no aggregator registered).
 */
public class ExecutionException extends Exception {
    public ExecutionException(String message) { super(message); }
    public ExecutionException(String message, Throwable cause) { super(message, cause); }
}
