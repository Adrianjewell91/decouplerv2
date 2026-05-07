package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Command} that reads rows matching a filter.
 */
public class SqlReadCommand implements Command {

    private final Map<String, Object> filter;

    public SqlReadCommand(Map<String, Object> filter) {
        this.filter = Map.copyOf(filter);
    }

    @Override
    public String build(Schema schema) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(schema.name());
        if (!filter.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(filter.keySet().stream()
                    .map(k -> k + " = ?")
                    .collect(Collectors.joining(" AND ")));
        }
        return sql.toString();
    }

    @Override
    public Map<String, Object> params() {
        return new LinkedHashMap<>(filter);
    }

    @Override
    public String describe() {
        return "READ: select rows from a fragment matching a filter";
    }
}
