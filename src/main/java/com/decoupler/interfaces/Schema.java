package com.decoupler.interfaces;

import java.util.List;

/**
 * Structural description of a {@link Fragment}.
 *
 * <p>Implementations may extend this to carry additional type metadata
 * (e.g. JDBC column types, JSON field types).
 */
public interface Schema {
    String name();
    List<FieldDescriptor> fields();

    /** Generic field descriptor: a named field with an optional type hint. */
    record FieldDescriptor(String name, String typeHint) {}
}
