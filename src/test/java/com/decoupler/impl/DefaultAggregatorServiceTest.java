package com.decoupler.impl;

import com.decoupler.interfaces.Aggregator;
import com.decoupler.interfaces.AggregatorService;
import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class DefaultAggregatorServiceTest {

    private static Aggregator stubAggregator(String sig) {
        return new Aggregator() {
            @Override public String getTxSig() { return sig; }
            @Override public boolean canHandle(Transaction tx) { return tx.id().equals(sig); }
            @Override public com.decoupler.interfaces.Result execute(Transaction tx, com.decoupler.interfaces.Partition p) { return null; }
        };
    }

    private static Transaction stubTx(String id) {
        return new Transaction() {
            @Override public String id() { return id; }
            @Override public String logic() { return ""; }
            @Override public Map<String, Object> params() { return Map.of(); }
        };
    }

    @Test
    void putThenGetByExactIdReturnsAggregator() {
        AggregatorService svc = new DefaultAggregatorService();
        Aggregator agg = stubAggregator("txA");
        svc.put(agg);

        assertThat(svc.get(stubTx("txA"))).isSameAs(agg);
    }

    @Test
    void getByCanHandleFallbackWhenIdDoesNotMatchExactly() {
        AggregatorService svc = new DefaultAggregatorService();
        Aggregator agg = stubAggregator("txA");
        svc.put(agg);

        Aggregator aggB = new Aggregator() {
            @Override public String getTxSig() { return "sigB"; }
            @Override public boolean canHandle(Transaction tx) { return tx.id().equals("txB"); }
            @Override public com.decoupler.interfaces.Result execute(Transaction tx, com.decoupler.interfaces.Partition p) { return null; }
        };
        svc.put(aggB);

        assertThat(svc.get(stubTx("txB"))).isSameAs(aggB);
    }

    @Test
    void getReturnsNullWhenNothingRegistered() {
        AggregatorService svc = new DefaultAggregatorService();
        assertThat(svc.get(stubTx("unknown"))).isNull();
    }

    @Test
    void secondPutWithSameSigReplacesFirst() {
        AggregatorService svc = new DefaultAggregatorService();
        Aggregator first = stubAggregator("txA");
        Aggregator second = stubAggregator("txA");
        svc.put(first);
        svc.put(second);

        assertThat(svc.get(stubTx("txA"))).isSameAs(second);
    }
}
