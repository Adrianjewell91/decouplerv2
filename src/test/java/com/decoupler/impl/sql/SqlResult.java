package com.decoupler.impl.sql;

import com.decoupler.interfaces.Result;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SQL-specific {@link Result}.
 *
 * <p>Either holds a list of rows (from a SELECT / read command) or a row-count
 * integer (from an INSERT / UPDATE / DELETE command).
 */
public final class SqlResult implements Result {

    private final Object message;

    private SqlResult(Object message) {
        this.message = message;
    }

    // -------------------------------------------------------------------------
    // Factories
    // -------------------------------------------------------------------------

    public static Result ofRows(List<Map<String, Object>> rows) {
        return new SqlResult(List.copyOf(rows));
    }

    public static Result ofCount(int count) {
        return new SqlResult(count);
    }

    // -------------------------------------------------------------------------
    // Result interface
    // -------------------------------------------------------------------------

    @Override
    public Object message() {
        return message;
    }

    // -------------------------------------------------------------------------
    // equals / hashCode
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SqlResult that)) return false;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(message);
    }

    @Override
    public String toString() {
        return "SqlResult{message=" + message + '}';
    }
}
