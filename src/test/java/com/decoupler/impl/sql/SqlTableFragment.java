package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Fragment;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Schema;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JDBC-backed {@link Fragment} for a single database table.
 *
 * <p>Two construction modes:
 * <ul>
 *   <li><b>Direct</b>: {@code SqlTableFragment(DataSource ds, String tableName)} — the
 *       caller provides the DataSource; {@code init()} is a no-op.
 *   <li><b>Replicated</b>: {@code SqlTableFragment(String tableName, FragmentReplicator replicator)}
 *       — {@code init()} creates a private in-memory H2 database and calls the replicator
 *       to copy data into it. Each fragment owns its own isolated database.
 * </ul>
 *
 * <p>{@code init()} is idempotent: calling it more than once has no effect.
 * {@code execute(command)} must not be called before {@code init()} in replicated mode.
 */
public class SqlTableFragment implements Fragment {

    private final String tableName;
    private final FragmentReplicator replicator;
    private final DataSourceFactory dataSourceFactory;
    private DataSource ownDataSource;
    private Schema cachedSchema;

    /** Direct mode — caller provides the DataSource; init() is a no-op. */
    public SqlTableFragment(DataSource dataSource, String tableName) {
        this.tableName = tableName.toLowerCase();
        this.ownDataSource = dataSource;
        this.replicator = null;
        this.dataSourceFactory = null;
    }

    /** Replicated mode — init() creates a DataSource via the factory and seeds it via the replicator. */
    public SqlTableFragment(String tableName, FragmentReplicator replicator, DataSourceFactory dataSourceFactory) {
        this.tableName = tableName.toLowerCase();
        this.replicator = replicator;
        this.dataSourceFactory = dataSourceFactory;
        this.ownDataSource = null;
    }

    // -------------------------------------------------------------------------
    // Fragment interface
    // -------------------------------------------------------------------------

    @Override
    public void init() {
        if (replicator == null || ownDataSource != null) return; // idempotent / direct mode
        ownDataSource = dataSourceFactory.create("fragment_" + tableName + "_" + randomSuffix());
        replicator.replicate(ownDataSource, tableName);
        cachedSchema = introspectSchema();
    }

    @Override
    public void close() {
        if (replicator != null && ownDataSource != null) {
            try (Connection c = ownDataSource.getConnection();
                 Statement s = c.createStatement()) {
                s.execute("DROP TABLE IF EXISTS " + tableName);
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result execute(Command command) {
        Schema s = schema();
        String sql = command.build(s);
        Map<String, Object> params = command.params();
        validateKeys(params, s);

        if (isRead(sql)) {
            try (Connection conn = ownDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                JdbcHelper.bindParams(ps, params.values());
                try (ResultSet rs = ps.executeQuery()) {
                    return SqlResult.ofRows(JdbcHelper.resultSetToList(rs));
                }
            } catch (SQLException e) {
                throw new RuntimeException("Read failed on table " + tableName, e);
            }
        } else {
            try (Connection conn = ownDataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                JdbcHelper.bindParams(ps, params.values());
                int affected = ps.executeUpdate();
                return SqlResult.ofCount(affected);
            } catch (SQLException e) {
                throw new RuntimeException("Write failed on table " + tableName, e);
            }
        }
    }

    @Override
    public Schema schema() {
        if (cachedSchema == null) {
            cachedSchema = introspectSchema();
        }
        return cachedSchema;
    }

    // -------------------------------------------------------------------------
    // Schema introspection
    // -------------------------------------------------------------------------

    private Schema introspectSchema() {
        List<Schema.FieldDescriptor> fields = new ArrayList<>();
        try (Connection conn = ownDataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            try (ResultSet cols = meta.getColumns(null, null, tableName.toUpperCase(), null)) {
                while (cols.next()) {
                    fields.add(new Schema.FieldDescriptor(
                            cols.getString("COLUMN_NAME").toLowerCase(),
                            cols.getString("TYPE_NAME")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Schema introspection failed for table " + tableName, e);
        }
        if (fields.isEmpty()) {
            throw new IllegalStateException(
                    "Table '" + tableName + "' not found or has no columns.");
        }
        return new SqlSchema(tableName, fields);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validateKeys(Map<String, Object> params, Schema schema) {
        if (params == null || params.isEmpty()) return;
        for (String key : params.keySet()) {
            boolean known = schema.fields().stream()
                    .anyMatch(f -> f.name().equalsIgnoreCase(key));
            if (!known) {
                throw new IllegalArgumentException(
                        "Unknown column '" + key + "' on table '" + tableName + "'. " +
                        "Valid columns: " + schema.fields().stream()
                                .map(Schema.FieldDescriptor::name)
                                .collect(Collectors.joining(", ")));
            }
        }
    }

    private boolean isRead(String sql) {
        return sql.stripLeading().toUpperCase().startsWith("SELECT");
    }

    private static String randomSuffix() {
        return Long.toHexString(System.nanoTime());
    }

    @Override
    public String toString() {
        return "SqlTableFragment{table='" + tableName + "'}";
    }
}
