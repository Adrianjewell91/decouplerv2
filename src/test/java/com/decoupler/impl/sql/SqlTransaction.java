package com.decoupler.impl.sql;

import com.decoupler.interfaces.Transaction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQL-backed {@link Transaction}.
 *
 * <p>{@link #logic()} holds the SQL string.
 * {@link #params()} is a {@link LinkedHashMap} keyed by positional index string
 * ("0", "1", "2", …) preserving insertion order.
 */
public final class SqlTransaction implements Transaction {

    private final String id;
    private final String logic;
    private final LinkedHashMap<String, Object> params;

    private SqlTransaction(String id, String logic, LinkedHashMap<String, Object> params) {
        this.id = id;
        this.logic = logic;
        this.params = params;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    public static Transaction of(String id, String sql, Object... params) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < params.length; i++) {
            map.put(String.valueOf(i), params[i]);
        }
        return new SqlTransaction(id, sql, map);
    }

    // -------------------------------------------------------------------------
    // Transaction interface
    // -------------------------------------------------------------------------

    @Override
    public String id() { return id; }

    @Override
    public String logic() { return logic; }

    @Override
    public Map<String, Object> params() {
        return Collections.unmodifiableMap(params);
    }

    @Override
    public String toString() {
        return "SqlTransaction{id='" + id + "', logic='" + logic + "', params=" + params + '}';
    }
}
