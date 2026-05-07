package com.decoupler.impl.sql;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Fragment;
import com.decoupler.interfaces.Monolith;
import com.decoupler.interfaces.Partition;
import com.decoupler.interfaces.Partitioner;
import com.decoupler.interfaces.Result;
import com.decoupler.interfaces.Transaction;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * JDBC-backed {@link Monolith}.
 *
 * <p>Executes transactions using {@link Transaction#logic()} for SQL and
 * {@link Transaction#params()} for bind values.
 */
public class SqlMonolith implements Monolith {

    private final DataSource dataSource;

    public SqlMonolith(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void init() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Monolith connectivity check failed", e);
        }
    }

    @Override
    public void close() { /* no-op */ }

    @Override
    public Result execute(Transaction tx) {
        if (isWrite(tx)) {
            return executeWrite(tx);
        } else {
            return executeRead(tx);
        }
    }

    private Result executeRead(Transaction tx) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(tx.logic())) {
            JdbcHelper.bindParams(ps, tx.params().values());
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> rows = JdbcHelper.resultSetToList(rs);
                return SqlResult.ofRows(rows);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Monolith read failed for transaction [" + tx.id() + "]", e);
        }
    }

    private Result executeWrite(Transaction tx) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(tx.logic(), Statement.RETURN_GENERATED_KEYS)) {
            JdbcHelper.bindParams(ps, tx.params().values());
            int affected = ps.executeUpdate();
            return SqlResult.ofCount(affected);
        } catch (SQLException e) {
            throw new RuntimeException("Monolith write failed for transaction [" + tx.id() + "]", e);
        }
    }

    @Override
    public Partition partition(Partitioner partitioner) {
        List<Fragment> fragments = partitioner.fragments();
        fragments.forEach(Fragment::init);
        List<Command> commands = List.of(
                new SqlReadCommand(Map.of()),
                new SqlInsertCommand(Map.of()),
                new SqlUpdateCommand(Map.of(), Map.of()),
                new SqlDeleteCommand(Map.of())
        );
        return new SqlPartition(fragments, commands);
    }


    // -------------------------------------------------------------------------
    // SQL-specific helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if this transaction's SQL is a write statement
     * (INSERT, UPDATE, or DELETE), case-insensitive.
     */
    private boolean isWrite(Transaction tx) {
        String upper = tx.logic().stripLeading().toUpperCase();
        return upper.startsWith("INSERT") || upper.startsWith("UPDATE") || upper.startsWith("DELETE");
    }
}
