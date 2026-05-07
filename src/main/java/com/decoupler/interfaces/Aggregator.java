package com.decoupler.interfaces;

/**
 * Stateless executor for a single transaction pattern in the decomposition layer.
 *
 * <p>Aggregators are constructed via the nested {@link Builder} interface, which
 * keeps construction (LLM interaction, plan parsing) separate from execution.
 */
public interface Aggregator {
    String getTxSig();
    boolean canHandle(Transaction tx);
    Result execute(Transaction tx, Partition partition);

    /**
     * Fluent builder for constructing an {@link Aggregator}.
     *
     * <p>Each parameter is set via its own method; {@link #build()} assembles
     * the aggregator (typically by prompting the {@link LlmClient}).
     * All parameters are required — {@link #build()} throws {@link BuildException}
     * if any are missing.
     */
    interface Builder {
        Builder transaction(Transaction tx);
        Builder partition(Partition partition);
        Builder llmClient(LlmClient llmClient);
        Builder timeout(java.time.Duration timeout);
        Aggregator build() throws BuildException;
    }
}
