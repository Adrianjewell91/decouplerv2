package com.decoupler.interfaces;

/**
 * Registry mapping transaction signatures to their {@link Aggregator}s.
 */
public interface AggregatorService {
    Aggregator get(Transaction tx);
    void put(Aggregator aggregator);
}
