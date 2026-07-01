package com.database.utils.structure;

import com.database.dto.request.info.DatabaseDbRunQueryRequest;
import com.database.dto.response.structure.DatabaseTableColumnMeta;
import com.database.dto.response.structure.run_query.DatabaseQueryResultResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DatabaseQueryExecutionUtility {

  private static final int DEFAULT_MAX_ROWS = 200;
  private static final int HARD_MAX_ROWS = 1000;
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;
  private static final int HARD_MAX_TIMEOUT_SECONDS = 300;

  public List<DatabaseQueryResultResponse> runQuery(
      Connection connection, DatabaseDbRunQueryRequest request) {

    int cap = clamp(request.maxRows, DEFAULT_MAX_ROWS, HARD_MAX_ROWS);
    int timeoutSeconds =
        clamp(request.timeoutSeconds, DEFAULT_TIMEOUT_SECONDS, HARD_MAX_TIMEOUT_SECONDS);

    long start = System.nanoTime();

    try (Statement st = connection.createStatement()) {

      st.setMaxRows(cap + 1);
      st.setQueryTimeout(timeoutSeconds);
      boolean isResultSet = st.execute(request.query);

      DatabaseQueryResultResponse result = isResultSet ? readRows(st, cap) : readUpdate(st);
      result.durationMs = elapsedMs(start);
      return List.of(result);
    } catch (SQLException e) {

      DatabaseQueryResultResponse err =
          DatabaseQueryResultResponse.ofError(e.getMessage(), e.getSQLState(), e.getErrorCode());
      err.durationMs = elapsedMs(start);
      return List.of(err);
    }
  }

  private DatabaseQueryResultResponse readRows(Statement st, int cap) throws SQLException {
    try (ResultSet rs = st.getResultSet()) {
      ResultSetMetaData md = rs.getMetaData();
      int colCount = md.getColumnCount();

      List<DatabaseTableColumnMeta> columns = new ArrayList<>(colCount);
      for (int i = 1; i <= colCount; i++) {

        columns.add(new DatabaseTableColumnMeta(md.getColumnLabel(i), md.getColumnTypeName(i)));
      }

      List<List<Object>> rows = new ArrayList<>();
      boolean truncated = false;
      while (rs.next()) {
        if (rows.size() >= cap) {
          truncated = true;
          break;
        }
        List<Object> row = new ArrayList<>(colCount);
        for (int i = 1; i <= colCount; i++) {
          row.add(safeValue(rs, md, i));
        }
        rows.add(row);
      }
      return DatabaseQueryResultResponse.ofRows(columns, rows, truncated);
    }
  }

  private DatabaseQueryResultResponse readUpdate(Statement st) throws SQLException {
    int affected = st.getUpdateCount();
    return DatabaseQueryResultResponse.ofUpdate(affected, "Statement executed successfully");
  }

  private Object safeValue(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
    int type = md.getColumnType(i);
    if (type == Types.BLOB
        || type == Types.BINARY
        || type == Types.VARBINARY
        || type == Types.LONGVARBINARY) {
      return rs.getObject(i) == null ? null : "[binary]";
    }
    return rs.getObject(i);
  }

  private int clamp(Integer requested, int dflt, int max) {
    if (requested == null || requested <= 0) return dflt;
    return Math.min(requested, max);
  }

  private long elapsedMs(long startNanos) {
    return (System.nanoTime() - startNanos) / 1_000_000;
  }
}
