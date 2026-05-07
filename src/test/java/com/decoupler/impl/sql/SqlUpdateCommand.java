package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Command} that updates rows matching a filter with new field values.
 */
public class SqlUpdateCommand implements Command {

    private final Map<String, Object> values;
    private final Map<String, Object> filter;

    public SqlUpdateCommand(Map<String, Object> values, Map<String, Object> filter) {
        this.values = Map.copyOf(values);
        this.filter = Map.copyOf(filter);
    }

    @Override
    public String build(Schema schema) {
        String setClause = values.keySet().stream()
                .map(k -> k + " = ?")
                .collect(Collectors.joining(", "));
        String whereClause = filter.isEmpty() ? "" :
                " WHERE " + filter.keySet().stream()
                        .map(k -> k + " = ?")
                        .collect(Collectors.joining(" AND "));
        return "UPDATE " + schema.name() + " SET " + setClause + whereClause;
    }

    @Override
    public Map<String, Object> params() {
        LinkedHashMap<String, Object> p = new LinkedHashMap<>(values);
        p.putAll(filter);
        return p;
    }

    @Override
    public String describe() {
        return "UPDATE: update rows in a fragment matching a filter with new field values";
    }
}
