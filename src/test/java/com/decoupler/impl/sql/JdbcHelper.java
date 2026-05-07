package com.decoupler.impl.sql;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Package-private JDBC utility helpers.
 */
final class JdbcHelper {

    private JdbcHelper() {}

    /**
     * Binds each value in {@code values} to the {@link PreparedStatement} at positions 1, 2, 3, …
     */
    static void bindParams(PreparedStatement ps, Collection<Object> values) throws SQLException {
        int i = 1;
        for (Object v : values) {
            ps.setObject(i++, v);
        }
    }

    /**
     * Reads all rows from {@code rs} into a list of maps.
     * Column names are lower-cased for consistency.
     */
    static List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(meta.getColumnName(i).toLowerCase(), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
