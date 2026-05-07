package com.decoupler.impl;

import com.decoupler.interfaces.Decomposition;
import com.decoupler.interfaces.DecouplingStatus;
import com.decoupler.interfaces.ExecutionException;
import com.decoupler.interfaces.Monolith;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Partitioner;
import com.decoupler.interfaces.Proxy;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultProxy implements Proxy {

    private final Partitioner partitioner;
    private Monolith monolith;
    private Decomposition decomposition;
    private Partition cachedPartition;

    private final Map<String, DecouplingStatus> statusMap = new ConcurrentHashMap<>();
    private final List<String> log = new CopyOnWriteArrayList<>();

    public DefaultProxy(Partitioner partitioner) {
        this.partitioner = partitioner;
    }

    @Override
    public void init(Monolith monolith, Decomposition decomposition) {
        this.monolith = monolith;
        this.decomposition = decomposition;
        this.cachedPartition = monolith.partition(partitioner);
    }

    @Override
    public Result process(Transaction tx) throws ExecutionException {
        DecouplingStatus status = statusMap.getOrDefault(tx.id(), DecouplingStatus.MONOLITH);
        return switch (status) {
            case MONOLITH -> {
                Result r = monolith.execute(tx);
                log.add("MONOLITH[" + tx.id() + "]");
                yield r;
            }
            case SHADOW -> {
                Result monolithResult = monolith.execute(tx);
                try {
                    Result decompResult = decomposition.execute(tx, cachedPartition);
                    if (!java.util.Objects.equals(monolithResult.message(), decompResult.message())) {
                        log.add("SHADOW[" + tx.id() + "]: MISMATCH monolith=" + monolithResult.message()
                                + " decomp=" + decompResult.message());
                    } else {
                        log.add("SHADOW[" + tx.id() + "]: match");
                    }
                } catch (ExecutionException e) {
                    log.add("SHADOW[" + tx.id() + "]: decomp error: " + e.getMessage());
                }
                yield monolithResult;
            }
            case DECOUPLED -> {
                Result r = decomposition.execute(tx, cachedPartition);
                log.add("DECOUPLED[" + tx.id() + "]");
                yield r;
            }
        };
    }

    @Override
    public DecouplingStatus promote(Transaction tx) {
        return statusMap.compute(tx.id(), (k, v) -> switch (v == null ? DecouplingStatus.MONOLITH : v) {
            case MONOLITH -> DecouplingStatus.SHADOW;
            case SHADOW -> DecouplingStatus.DECOUPLED;
            case DECOUPLED -> DecouplingStatus.DECOUPLED;
        });
    }

    @Override
    public List<String> inspect() {
        return Collections.unmodifiableList(log);
    }

    @Override
    public void clearInspect() {
        log.clear();
    }

    public Partition getCachedPartition() {
        return cachedPartition;
    }
}
