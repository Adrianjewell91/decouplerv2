package com.decoupler.impl.sql;

import javax.sql.DataSource;

/**
 * Strategy that seeds a {@link SqlTableFragment}'s own {@link DataSource} with data
 * from an external source during {@link SqlTableFragment#init()}.
 *
 * <p>The fragment supplies the freshly-created target DataSource and the table name;
 * the implementation is responsible for DDL and data transfer.
 */
@FunctionalInterface
public interface FragmentReplicator {
    void replicate(DataSource target, String tableName);
}
