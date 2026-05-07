package com.decoupler.interfaces;

/**
 * Thrown when an {@link Aggregator.Builder} cannot construct an {@link Aggregator},
 * typically because the LLM returned an unparseable or invalid response.
 */
public class BuildException extends Exception {
    public BuildException(String message) { super(message); }
    public BuildException(String message, Throwable cause) { super(message, cause); }
}
