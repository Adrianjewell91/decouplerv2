package com.decoupler.integration;

import com.decoupler.impl.DefaultAggregatorService;
import com.decoupler.impl.DefaultDecomposition;
import com.decoupler.impl.DefaultProxy;
import com.decoupler.impl.llm.OllamaLlmClient;
import com.decoupler.impl.sql.SqlMonolith;
import com.decoupler.impl.sql.SqlPartitioner;
import com.decoupler.impl.sql.H2TestSupport;
import com.decoupler.interfaces.*;

import javax.sql.DataSource;

/**
 * Single assembly point for integration tests.
 *
 * <p>All references to concrete implementation classes are confined here.
 */
public final class DecouplerFactory {

    private DecouplerFactory() {}

    public static Monolith createMonolith(DataSource ds) {
        return new SqlMonolith(ds);
    }

    public static Partitioner createPartitioner(DataSource ds) {
        return new SqlPartitioner(ds, H2TestSupport::createDataSource);
    }

    public static DefaultDecomposition createDecomposition(AggregatorService aggregatorService) {
        return new DefaultDecomposition(aggregatorService);
    }

    public static DefaultProxy createProxy(Partitioner partitioner) {
        return new DefaultProxy(partitioner);
    }

    public static LlmClient createLlmClient() {
        return new OllamaLlmClient();
    }

    public static AggregatorService createAggregatorService() {
        return new DefaultAggregatorService();
    }
}
