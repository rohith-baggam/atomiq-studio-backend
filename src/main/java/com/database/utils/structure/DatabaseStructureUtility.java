package com.database.utils.structure;

import com.database.dto.request.info.DatabaseDbTableNameRequest;
import com.database.dto.response.structure.DatabaseTableColumnMeta;
import com.database.dto.response.structure.DatabaseTableDataResponse;
import com.database.dto.response.structure.DatabaseTableListResponse;
import com.database.dto.response.structure.DatabaseTablePropertiesColumnResource;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class DatabaseStructureUtility {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public List<DatabaseTableListResponse> getDatabaseTableList(Connection connection)
      throws SQLException {
    List<DatabaseTableListResponse> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    List<String[]> tables = new ArrayList<>();
    try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        tables.add(new String[] {rs.getString("TABLE_SCHEM"), rs.getString("TABLE_NAME")});
      }
    }

    for (String[] t : tables) {
      String schema = t[0];
      String tableName = t[1];
      String qualified = "\"" + schema + "\".\"" + tableName + "\"";

      long rowCount = 0;
      try (Statement st = connection.createStatement();
          ResultSet countRs = st.executeQuery("SELECT COUNT(*) FROM " + qualified)) {
        countRs.next();
        rowCount = countRs.getLong(1);
      }

      int columnCount = 0;
      try (Statement st = connection.createStatement();
          ResultSet colRs = st.executeQuery("SELECT * FROM " + qualified + " LIMIT 0")) {
        columnCount = colRs.getMetaData().getColumnCount();
      }

      results.add(new DatabaseTableListResponse(tableName, rowCount, columnCount));
    }

    return results;
  }

  public boolean isTableNameExist(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] {"TABLE"})) {
      return rs.next();
    }
  }

  public List<DatabaseTablePropertiesColumnResource> getDatabaseTableColumnPropertyDetails(
      Connection connection, String tableName) throws SQLException {

    if (!this.isTableNameExist(connection, tableName)) {
      throw new ValidationException("Invalid tableName");
    }
    List<DatabaseTablePropertiesColumnResource> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    // 1. primary-key columns
    Set<String> pkCols = new HashSet<>();
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
    }

    // 2. foreign-key columns
    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next()) fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    // 3. indexed + unique columns
    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null) continue; // stats row → skip
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE")) uniqueCols.add(col);
      }
    }

    // 4. columns — merge the sets in
    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        String columnName = rs.getString("COLUMN_NAME");
        results.add(
            new DatabaseTablePropertiesColumnResource(
                columnName,
                rs.getString("TYPE_NAME"),
                rs.getInt("COLUMN_SIZE"),
                rs.getInt("DECIMAL_DIGITS"),
                rs.getInt("ORDINAL_POSITION"),
                rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls,
                rs.getString("COLUMN_DEF"),
                rs.getString("IS_AUTOINCREMENT"),
                rs.getString("REMARKS"),
                pkCols.contains(columnName),
                fkCols.contains(columnName),
                indexedCols.contains(columnName),
                uniqueCols.contains(columnName)));
      }
    }

    return results;
  }

  public DatabaseTableDataResponse getDbTabledata(
      Connection connection, DatabaseDbTableNameRequest request) throws SQLException {

    if (!isTableNameExist(connection, request.tableName)) {
      throw new ValidationException("Invalid tableName");
    }

    String table = "\"" + request.tableName + "\"";

    // 1. columns + ORDER BY whitelist
    List<DatabaseTableColumnMeta> columns = new ArrayList<>();
    Set<String> columnNames = new HashSet<>();
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " + table + " LIMIT 0")) {
      ResultSetMetaData md = rs.getMetaData();
      for (int i = 1; i <= md.getColumnCount(); i++) {
        columns.add(new DatabaseTableColumnMeta(md.getColumnName(i), md.getColumnTypeName(i)));
        columnNames.add(md.getColumnName(i));
      }
    }

    // 2. total row count
    long count = 0;
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
      rs.next();
      count = rs.getLong(1);
    }

    // 3. sanitize limit/offset
    int limit = (request.limit == null || request.limit <= 0) ? 10 : Math.min(request.limit, 1000);
    int offset = (request.offset == null || request.offset < 0) ? 0 : request.offset;

    // 4. sorting (whitelisted)
    String orderBy = "";
    if (request.sortBy != null && !request.sortBy.isBlank()) {
      if (!columnNames.contains(request.sortBy)) {
        throw new ValidationException("Invalid sort column: " + request.sortBy);
      }
      String dir = "DESC".equalsIgnoreCase(request.sortDir) ? "DESC" : "ASC";
      orderBy = " ORDER BY \"" + request.sortBy + "\" " + dir;
    }

    // 5. paged query
    String sql = "SELECT * FROM " + table + orderBy + " LIMIT ? OFFSET ?";

    List<List<Object>> rows = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setInt(1, limit);
      ps.setInt(2, offset);
      try (ResultSet rs = ps.executeQuery()) {
        int colCount = rs.getMetaData().getColumnCount();
        while (rs.next()) {
          List<Object> row = new ArrayList<>(colCount);
          for (int i = 1; i <= colCount; i++) {
            row.add(rs.getObject(i));
          }
          rows.add(row);
        }
      }
    }

    // 6. paging flags — derived from count (Django LimitOffsetPagination style)
    boolean hasNext = offset + rows.size() < count; // more rows beyond this window
    boolean hasPrev = offset > 0;

    return new DatabaseTableDataResponse(columns, rows, hasNext, hasPrev, count);
  }
}
