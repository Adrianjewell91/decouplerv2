package com.decoupler.impl.sql;

import javax.sql.DataSource;

/**
 * Creates a named {@link DataSource} for use by a {@link SqlTableFragment} in replicated mode.
 *
 * <p>The name is typically derived from the table name and a random suffix to ensure isolation.
 * The choice of database engine (H2, Derby, SQLite, etc.) is entirely up to the caller,
 * keeping that dependency out of the main framework code.
 */
@FunctionalInterface
public interface DataSourceFactory {
    DataSource create(String name);
}
