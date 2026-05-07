package com.decoupler.impl.sql;

import com.decoupler.interfaces.Schema;

import java.util.List;

/**
 * Immutable, package-private SQL schema for a single table fragment.
 */
final class SqlSchema implements Schema {

    private final String name;
    private final List<FieldDescriptor> fields;

    SqlSchema(String name, List<FieldDescriptor> fields) {
        this.name = name;
        this.fields = List.copyOf(fields);
    }

    @Override
    public String name() { return name; }

    @Override
    public List<FieldDescriptor> fields() { return fields; }

    @Override
    public String toString() {
        return "SqlSchema{name='" + name + "', fields=" + fields + '}';
    }
}
