package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Schema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Command} that inserts a new row with the given field values.
 */
public class SqlInsertCommand implements Command {

    private final Map<String, Object> values;

    public SqlInsertCommand(Map<String, Object> values) {
        this.values = Map.copyOf(values);
    }

    @Override
    public String build(Schema schema) {
        String cols = String.join(", ", values.keySet());
        String placeholders = values.keySet().stream().map(k -> "?").collect(Collectors.joining(", "));
        return "INSERT INTO " + schema.name() + " (" + cols + ") VALUES (" + placeholders + ")";
    }

    @Override
    public Map<String, Object> params() {
        return new LinkedHashMap<>(values);
    }

    @Override
    public String describe() {
        return "INSERT: insert a new row into a fragment with given field values";
    }
}
