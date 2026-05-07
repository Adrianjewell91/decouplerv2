package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Command} that deletes rows matching a filter.
 */
public class SqlDeleteCommand implements Command {

    private final Map<String, Object> filter;

    public SqlDeleteCommand(Map<String, Object> filter) {
        this.filter = Map.copyOf(filter);
    }

    @Override
    public String build(Schema schema) {
        String whereClause = filter.isEmpty() ? "" :
                " WHERE " + filter.keySet().stream()
                        .map(k -> k + " = ?")
                        .collect(Collectors.joining(" AND "));
        return "DELETE FROM " + schema.name() + whereClause;
    }

    @Override
    public Map<String, Object> params() {
        return new LinkedHashMap<>(filter);
    }

    @Override
    public String describe() {
        return "DELETE: delete rows from a fragment matching a filter";
    }
}
