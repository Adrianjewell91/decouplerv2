package com.decoupler.impl;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.AggregatorService;
import com.decoupler.interfaces.BuildException;
import com.decoupler.interfaces.Decomposition;
import com.decoupler.interfaces.ExecutionException;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;

/**
 * Default {@link Decomposition} implementation.
 *
 * <p>Delegates aggregator construction to an {@link Aggregator.Builder} and
 * aggregator lookup to a shared {@link AggregatorService}.
 */
public class DefaultDecomposition implements Decomposition {

    private final AggregatorService service;

    public DefaultDecomposition(AggregatorService service) {
        this.service = service;
    }

    @Override
    public Aggregator build(Aggregator.Builder builder) throws BuildException {
        return builder.build();
    }

    @Override
    public void register(Aggregator aggregator, AggregatorService svc) {
        svc.put(aggregator);
    }

    @Override
    public Result execute(Transaction tx, Partition partition) throws ExecutionException {
        Aggregator aggregator = service.get(tx);
        if (aggregator == null) {
            throw new ExecutionException(
                    "No aggregator registered for transaction id='" + tx.id() + "'");
        }
        return aggregator.execute(tx, partition);
    }
}
