package com.decoupler.impl;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.AggregatorService;
import com.decoupler.interfaces.Transaction;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default in-memory {@link AggregatorService} backed by a {@link ConcurrentHashMap}
 * keyed on transaction signature (= {@link Aggregator#getTxSig()}).
 */
public class DefaultAggregatorService implements AggregatorService {

    private final ConcurrentHashMap<String, Aggregator> registry = new ConcurrentHashMap<>();

    @Override
    public void put(Aggregator aggregator) {
        registry.put(aggregator.getTxSig(), aggregator);
    }

    @Override
    public Aggregator get(Transaction tx) {
        // 1. Exact key lookup
        Aggregator exact = registry.get(tx.id());
        if (exact != null) return exact;

        // 2. Scan for canHandle match
        for (Aggregator aggregator : registry.values()) {
            if (aggregator.canHandle(tx)) return aggregator;
        }

        return null;
    }
}
