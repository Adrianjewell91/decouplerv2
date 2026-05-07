package com.decoupler.interfaces;

/**
 * The decomposition side of the proxy: routes transactions to aggregators,
 * builds new aggregators on demand, and manages the aggregator registry.
 */
public interface Decomposition {
    Result execute(Transaction tx, Partition partition) throws ExecutionException;
    Aggregator build(Aggregator.Builder builder) throws BuildException;
    void register(Aggregator aggregator, AggregatorService service);
}
