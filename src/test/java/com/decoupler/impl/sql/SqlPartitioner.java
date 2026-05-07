package com.decoupler.impl.sql;

import com.decoupler.interfaces.Fragment;
import com.decoupler.interfaces.Partitioner;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Discovers all user tables via {@link DatabaseMetaData} and returns one
 * {@link SqlTableFragment} per table in replicated mode.
 *
 * <p>The fragment list is computed once and cached. Subsequent calls to
 * {@code fragments()} return the same instances, so the {@link com.decoupler.interfaces.Monolith}
 * and any {@link com.decoupler.interfaces.Aggregator} builder that both call
 * {@code partition(partitioner)} work against the same already-initialised fragments.
 *
 * <p>Each fragment is constructed with a {@link JdbcTableReplicator} backed by this
 * partitioner's DataSource. When {@link com.decoupler.interfaces.Monolith#partition} calls
 * {@code fragment.init()}, the fragment creates its own H2 database and replicates data from
 * the monolith.
 */
public class SqlPartitioner implements Partitioner {

    private final DataSource dataSource;
    private final FragmentReplicator replicator;
    private final DataSourceFactory dataSourceFactory;
    private List<Fragment> cachedFragments;

    public SqlPartitioner(DataSource dataSource, DataSourceFactory dataSourceFactory) {
        this.dataSource = dataSource;
        this.replicator = new JdbcTableReplicator(dataSource);
        this.dataSourceFactory = dataSourceFactory;
    }

    @Override
    public List<Fragment> fragments() {
        if (cachedFragments == null) {
            cachedFragments = buildFragments();
        }
        return cachedFragments;
    }

    private List<Fragment> buildFragments() {
        List<Fragment> fragments = new ArrayList<>();
        for (String table : discoverTables()) {
            fragments.add(new SqlTableFragment(table, replicator, dataSourceFactory));
        }
        return List.copyOf(fragments);
    }

    private List<String> discoverTables() {
        List<String> names = new ArrayList<>();
        try (Connection conn = dataSource.getConnection()) {
            String schema = conn.getSchema();
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet rs = meta.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    names.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to discover tables", e);
        }
        return names;
    }
}
