package com.decoupler.interfaces;

/**
 * Abstraction over a large-language-model backend.
 *
 * <p>How the raw response is interpreted (e.g. parsed into a decomposition plan)
 * is entirely the concern of the {@link Aggregator.Builder} implementation.
 */
public interface LlmClient {
    String generate(String prompt, java.time.Duration timeout);
}
