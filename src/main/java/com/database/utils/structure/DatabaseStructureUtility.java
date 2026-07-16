package com.database.utils.structure;

import com.database.dto.request.info.DatabaseDbTableDataRequest;
import com.database.dto.request.info.table_properties.DatabaseTableDetailResponse;
import com.database.dto.request.info.table_properties.DatabaseTableIndexResponse;
import com.database.dto.request.info.table_properties.DatabaseTableInfoResponse;
import com.database.dto.response.info.DatabaseTableFieldsHelperListResponse;
import com.database.dto.response.structure.DatabaseSchemaTableListResponse;
import com.database.dto.response.structure.DatabaseTableColumnMeta;
import com.database.dto.response.structure.DatabaseTableDataResponse;
import com.database.dto.response.structure.DatabaseTableDependentTableResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class DatabaseStructureUtility {

  @Inject
  DatabaseConnectionUtility databaseConnectionUtility;

  public List<DatabaseSchemaTableListResponse> getDatabaseTableList(Connection connection)
      throws SQLException {

    List<DatabaseSchemaTableListResponse> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    try (ResultSet schemaRs = meta.getSchemas()) {

      while (schemaRs.next()) {

        String schemaName = schemaRs.getString("TABLE_SCHEM");

        if ("information_schema".equalsIgnoreCase(schemaName)
            || schemaName.startsWith("pg_")) {
          continue;
        }

        List<DatabaseTableListResponse> tables = new ArrayList<>();

        try (ResultSet tableRs = meta.getTables(null, schemaName, "%", new String[] { "TABLE" })) {

          while (tableRs.next()) {

            String tableName = tableRs.getString("TABLE_NAME");
            String qualifiedTable = "\"" + schemaName + "\".\"" + tableName + "\"";

            long rowCount = 0;
            try (Statement st = connection.createStatement();
                ResultSet countRs = st.executeQuery("SELECT COUNT(*) FROM " + qualifiedTable)) {

              if (countRs.next()) {
                rowCount = countRs.getLong(1);
              }
            }

            int columnCount = 0;
            try (ResultSet columnRs = meta.getColumns(null, schemaName, tableName, "%")) {

              while (columnRs.next()) {
                columnCount++;
              }
            }

            tables.add(
                new DatabaseTableListResponse(
                    tableName,
                    rowCount,
                    columnCount));
          }
        }

        results.add(
            new DatabaseSchemaTableListResponse(
                schemaName,
                tables));
      }
    }

    return results;
  }

  public boolean isTableNameExist(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
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

    Set<String> pkCols = new HashSet<>();
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next())
        pkCols.add(rs.getString("COLUMN_NAME"));
    }

    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next())
        fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null)
          continue;
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE"))
          uniqueCols.add(col);
      }
    }

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

  public List<DatabaseTableFieldsHelperListResponse> tableFieldHelperList(
      Connection connection, String tableName) throws SQLException {

    if (!this.isTableNameExist(connection, tableName)) {
      throw new ValidationException("Invalid tableName");
    }
    List<DatabaseTableFieldsHelperListResponse> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    Set<String> pkCols = new HashSet<>();
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next())
        pkCols.add(rs.getString("COLUMN_NAME"));
    }

    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next())
        fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null)
          continue;
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE"))
          uniqueCols.add(col);
      }
    }

    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        String columnName = rs.getString("COLUMN_NAME");
        results.add(
            new DatabaseTableFieldsHelperListResponse(columnName, rs.getString("TYPE_NAME")));
      }
    }

    return results;
  }

  public DatabaseTableDataResponse getDbTabledata(
      Connection connection, DatabaseDbTableDataRequest request) throws SQLException {

    if (!isTableNameExist(connection, request.tableName)) {
      throw new ValidationException("Invalid tableName");
    }

    String table = "\"" + request.tableName + "\"";

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

    long count = 0;
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
      rs.next();
      count = rs.getLong(1);
    }

    int limit = (request.limit == null || request.limit <= 0) ? 10 : Math.min(request.limit, 1000);
    int offset = (request.offset == null || request.offset < 0) ? 0 : request.offset;

    String orderBy = "";
    if (request.sortBy != null && !request.sortBy.isBlank()) {
      if (!columnNames.contains(request.sortBy)) {
        throw new ValidationException("Invalid sort column: " + request.sortBy);
      }
      String dir = "DESC".equalsIgnoreCase(request.sortDir) ? "DESC" : "ASC";
      orderBy = " ORDER BY \"" + request.sortBy + "\" " + dir;
    }

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

    boolean hasNext = offset + rows.size() < count;
    boolean hasPrev = offset > 0;

    return new DatabaseTableDataResponse(columns, rows, hasNext, hasPrev, count);
  }

  public DatabaseTableDetailResponse getDbTableDetails(
      Connection connection, String tableName, DatabaseTableDependentTableResponse dependencies)
      throws SQLException {

    if (!isTableNameExist(connection, tableName)) {
      throw new ValidationException("Invalid tableName");
    }

    DatabaseMetaData meta = connection.getMetaData();

    // indexes — group getIndexInfo rows by INDEX_NAME
    Map<String, DatabaseTableIndexResponse> indexMap = new LinkedHashMap<>();
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
      while (rs.next()) {
        String indexName = rs.getString("INDEX_NAME");
        String col = rs.getString("COLUMN_NAME");
        if (indexName == null || col == null) {
          continue;
        }
        boolean unique = !rs.getBoolean("NON_UNIQUE");
        DatabaseTableIndexResponse idx = indexMap.computeIfAbsent(
            indexName, n -> new DatabaseTableIndexResponse(n, new ArrayList<>(), unique, null));
        idx.columns.add(col);
      }
    }
    List<DatabaseTableIndexResponse> indexes = new ArrayList<>(indexMap.values());

    // table identity + comment
    String schema = null;
    String tableType = null;
    String comment = null;
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
      if (rs.next()) {
        schema = rs.getString("TABLE_SCHEM");
        tableType = rs.getString("TABLE_TYPE");
        comment = rs.getString("REMARKS");
      }
    }

    // row count + column count
    String qualified = (schema != null) ? "\"" + schema + "\".\"" + tableName + "\"" : "\"" + tableName + "\"";
    long rowCount = 0;
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + qualified)) {
      rs.next();
      rowCount = rs.getLong(1);
    }

    int columnCount = 0;
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " + qualified + " LIMIT 0")) {
      columnCount = rs.getMetaData().getColumnCount();
    }

    DatabaseTableInfoResponse info = new DatabaseTableInfoResponse(
        tableName, schema, tableType, comment, rowCount, columnCount, indexes.size());

    String createTableDdl = this.getCreateTableDdl(connection, tableName);

    return new DatabaseTableDetailResponse(info, indexes, dependencies, createTableDdl);
  }

  public String getCreateTableDdl(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();

    String schema = null;
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] { "TABLE" })) {
      if (rs.next()) {
        schema = rs.getString("TABLE_SCHEM");
      }
    }
    String qualified = (schema != null) ? "\"" + schema + "\".\"" + tableName + "\"" : "\"" + tableName + "\"";

    List<String> defs = new ArrayList<>();
    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        String name = rs.getString("COLUMN_NAME");
        String type = rs.getString("TYPE_NAME");
        int size = rs.getInt("COLUMN_SIZE");
        int scale = rs.getInt("DECIMAL_DIGITS");
        boolean notNull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
        String columnDefault = rs.getString("COLUMN_DEF");

        StringBuilder col = new StringBuilder();
        col.append("  \"").append(name).append("\" ").append(renderType(type, size, scale));
        if (columnDefault != null && !columnDefault.isBlank()) {
          col.append(" DEFAULT ").append(columnDefault);
        }
        if (notNull) {
          col.append(" NOT NULL");
        }
        defs.add(col.toString());
      }
    }

    Map<Short, String> pkBySeq = new TreeMap<>();
    String pkName = null;
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next()) {
        pkBySeq.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
        pkName = rs.getString("PK_NAME");
      }
    }
    if (!pkBySeq.isEmpty()) {
      String cols = pkBySeq.values().stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", "));
      defs.add("  CONSTRAINT \"" + pkName + "\" PRIMARY KEY (" + cols + ")");
    }

    return "CREATE TABLE " + qualified + " (\n" + String.join(",\n", defs) + "\n);";
  }

  private String renderType(String type, int size, int scale) {
    String t = type.toLowerCase();
    if (t.contains("char") && size > 0 && size < Integer.MAX_VALUE) {
      return type + "(" + size + ")";
    }
    if ((t.equals("numeric") || t.equals("decimal")) && size > 0) {
      return scale > 0 ? type + "(" + size + ", " + scale + ")" : type + "(" + size + ")";
    }
    return type;
  }
}
