package com.decoupler.impl.decomposition;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;

/**
 * {@link Aggregator} that executes a {@link DecompositionPlan} via a {@link PlanInterpreter}.
 */
public class PlanBasedAggregator implements Aggregator {

    private final DecompositionPlan plan;
    private final PlanInterpreter interpreter;

    public PlanBasedAggregator(DecompositionPlan plan, PlanInterpreter interpreter) {
        this.plan = plan;
        this.interpreter = interpreter;
    }

    @Override
    public String getTxSig() {
        return plan.getQueryPattern();
    }

    @Override
    public boolean canHandle(Transaction tx) {
        return tx.id().equals(plan.getQueryPattern());
    }

    @Override
    public Result execute(Transaction tx, Partition partition) {
        return interpreter.execute(tx, plan, partition);
    }
}
