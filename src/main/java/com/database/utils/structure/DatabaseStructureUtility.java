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

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  private enum SqlDialect {
    POSTGRES,
    MYSQL,
    MSSQL,
    ORACLE
  }

  private SqlDialect getSqlDialect(Connection connection) throws SQLException {
    String productName = connection.getMetaData().getDatabaseProductName();
    if (productName == null) {
      return SqlDialect.POSTGRES;
    }
    String product = productName.toLowerCase();
    if (product.contains("microsoft")) {
      return SqlDialect.MSSQL;
    }
    if (product.contains("oracle")) {
      return SqlDialect.ORACLE;
    }
    if (product.contains("mysql")) {
      return SqlDialect.MYSQL;
    }
    return SqlDialect.POSTGRES;
  }

  /**
   * Wrap a (already quoted) column in a dialect-appropriate cast to text so case-insensitive LIKE/=
   * search works on any column type, not just strings.
   */
  private String castToText(SqlDialect dialect, String quotedCol) {
    switch (dialect) {
      case MYSQL:
        return "CAST(" + quotedCol + " AS CHAR)";
      case MSSQL:
        return "CAST(" + quotedCol + " AS NVARCHAR(MAX))";
      case ORACLE:
        return "TO_CHAR(" + quotedCol + ")";
      case POSTGRES:
      default:
        return "CAST(" + quotedCol + " AS TEXT)";
    }
  }

  public List<DatabaseSchemaTableListResponse> getDatabaseTableList(Connection connection)
      throws SQLException {

    List<DatabaseSchemaTableListResponse> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();
    SqlDialect dialect = getSqlDialect(connection);

    // Oracle equates schemas with database users, so getSchemas() returns dozens of
    // Oracle-maintained system users (SYS, MDSYS, XDB, ...). Querying those directly can
    // throw (e.g. ORA-22812 on nested-table system objects), so only list user-created ones.
    Set<String> oracleUserSchemas = null;
    if (dialect == SqlDialect.ORACLE) {
      oracleUserSchemas = new HashSet<>();
      try (Statement st = connection.createStatement();
          ResultSet rs =
              st.executeQuery("SELECT USERNAME FROM ALL_USERS WHERE ORACLE_MAINTAINED = 'N'")) {
        while (rs.next()) {
          oracleUserSchemas.add(rs.getString("USERNAME"));
        }
      }
    }

    try (ResultSet schemaRs = meta.getSchemas()) {

      while (schemaRs.next()) {

        String schemaName = schemaRs.getString("TABLE_SCHEM");

        if ("information_schema".equalsIgnoreCase(schemaName) || schemaName.startsWith("pg_")) {
          continue;
        }
        if (oracleUserSchemas != null && !oracleUserSchemas.contains(schemaName)) {
          continue;
        }

        List<DatabaseTableListResponse> tables = new ArrayList<>();

        try (ResultSet tableRs = meta.getTables(null, schemaName, "%", new String[] {"TABLE"})) {

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

            tables.add(new DatabaseTableListResponse(tableName, rowCount, columnCount));
          }
        }

        results.add(new DatabaseSchemaTableListResponse(schemaName, tables));
      }
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

    Set<String> pkCols = new HashSet<>();
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
    }

    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next()) fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    // approximate=true: read existing dictionary stats instead of forcing a fresh
    // ANALYZE. On Oracle, approximate=false makes the driver run DBMS_STATS, which
    // throws ORA-20000 ("insufficient privileges") on tables the user can't analyze;
    // other dialects ignore this flag, so true is safe everywhere and cheaper.
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, true)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null) continue;
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE")) uniqueCols.add(col);
      }
    }

    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        // Read columns in the JDBC spec's physical result-set order (COLUMN_NAME,
        // TYPE_NAME, COLUMN_SIZE, DECIMAL_DIGITS, NULLABLE, REMARKS, COLUMN_DEF,
        // ORDINAL_POSITION, IS_AUTOINCREMENT). Oracle exposes COLUMN_DEF as a
        // forward-only LONG stream, so reading out of order (e.g. REMARKS after
        // COLUMN_DEF) throws ORA-17027 "Stream has already been closed".
        String columnName = rs.getString("COLUMN_NAME");
        String typeName = rs.getString("TYPE_NAME");
        int columnSize = rs.getInt("COLUMN_SIZE");
        int decimalDigits = rs.getInt("DECIMAL_DIGITS");
        boolean notNull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
        String remarks = rs.getString("REMARKS");
        String columnDefault = rs.getString("COLUMN_DEF");
        int ordinalPosition = rs.getInt("ORDINAL_POSITION");
        String isAutoIncrement = rs.getString("IS_AUTOINCREMENT");

        results.add(
            new DatabaseTablePropertiesColumnResource(
                columnName,
                typeName,
                columnSize,
                decimalDigits,
                ordinalPosition,
                notNull,
                columnDefault,
                isAutoIncrement,
                remarks,
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
      while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
    }

    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next()) fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    // approximate=true: read existing dictionary stats instead of forcing a fresh
    // ANALYZE. On Oracle, approximate=false makes the driver run DBMS_STATS, which
    // throws ORA-20000 ("insufficient privileges") on tables the user can't analyze;
    // other dialects ignore this flag, so true is safe everywhere and cheaper.
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, true)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null) continue;
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE")) uniqueCols.add(col);
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
    SqlDialect dialect = getSqlDialect(connection);

    List<DatabaseTableColumnMeta> columns = new ArrayList<>();
    Set<String> columnNames = new HashSet<>();
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getColumns(null, null, request.tableName, "%")) {
      while (rs.next()) {
        String columnName = rs.getString("COLUMN_NAME");
        columns.add(new DatabaseTableColumnMeta(columnName, rs.getString("TYPE_NAME")));
        columnNames.add(columnName);
      }
    }

    // --- WHERE: global search + per-column filters (Phase 1) ----------------
    // Column names are whitelisted against the table's real columns; every value
    // is bound as a prepared-statement parameter, so nothing user-supplied is
    // concatenated into the SQL text.
    List<String> clauses = new ArrayList<>();
    List<Object> whereParams = new ArrayList<>();

    int filterCount = request.filterColumns == null ? 0 : request.filterColumns.size();
    for (int i = 0; i < filterCount; i++) {
      String col = request.filterColumns.get(i);
      if (col == null || !columnNames.contains(col)) {
        throw new ValidationException("Invalid filter column: " + col);
      }
      String op =
          (request.filterOps != null
                  && i < request.filterOps.size()
                  && request.filterOps.get(i) != null)
              ? request.filterOps.get(i)
              : "contains";
      String val =
          (request.filterValues != null && i < request.filterValues.size())
              ? request.filterValues.get(i)
              : "";
      if (val == null) {
        val = "";
      }
      String qcol = "\"" + col + "\"";
      String textCol = castToText(dialect, qcol);
      switch (op) {
        case "eq":
          clauses.add("LOWER(" + textCol + ") = LOWER(?)");
          whereParams.add(val);
          break;
        case "neq":
          clauses.add("LOWER(" + textCol + ") <> LOWER(?)");
          whereParams.add(val);
          break;
        case "isNull":
          clauses.add(qcol + " IS NULL");
          break;
        case "notNull":
          clauses.add(qcol + " IS NOT NULL");
          break;
        case "contains":
        default:
          clauses.add("LOWER(" + textCol + ") LIKE ?");
          whereParams.add("%" + val.toLowerCase() + "%");
          break;
      }
    }

    if (request.search != null && !request.search.isBlank()) {
      String needle = "%" + request.search.toLowerCase() + "%";
      List<String> orClauses = new ArrayList<>();
      for (String col : columnNames) {
        orClauses.add("LOWER(" + castToText(dialect, "\"" + col + "\"") + ") LIKE ?");
        whereParams.add(needle);
      }
      if (!orClauses.isEmpty()) {
        clauses.add("(" + String.join(" OR ", orClauses) + ")");
      }
    }

    String whereSql = clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);

    long count = 0;
    try (PreparedStatement ps =
        connection.prepareStatement("SELECT COUNT(*) FROM " + table + whereSql)) {
      for (int i = 0; i < whereParams.size(); i++) {
        ps.setObject(i + 1, whereParams.get(i));
      }
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        count = rs.getLong(1);
      }
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

    boolean useOffsetFetch = dialect == SqlDialect.MSSQL || dialect == SqlDialect.ORACLE;
    String sql;
    if (useOffsetFetch) {
      // MSSQL requires an ORDER BY clause for OFFSET/FETCH; Oracle's row-limiting clause
      // works without one, but a stable order avoids depending on physical storage order.
      String effectiveOrderBy =
          (dialect == SqlDialect.MSSQL && orderBy.isEmpty()) ? " ORDER BY (SELECT NULL)" : orderBy;
      sql =
          "SELECT * FROM "
              + table
              + whereSql
              + effectiveOrderBy
              + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
    } else {
      sql = "SELECT * FROM " + table + whereSql + orderBy + " LIMIT ? OFFSET ?";
    }

    List<List<Object>> rows = new ArrayList<>();
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      int idx = 1;
      for (Object p : whereParams) {
        ps.setObject(idx++, p);
      }
      if (useOffsetFetch) {
        ps.setInt(idx++, offset);
        ps.setInt(idx, limit);
      } else {
        ps.setInt(idx++, limit);
        ps.setInt(idx, offset);
      }
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
    // approximate=true: read existing dictionary stats instead of forcing a fresh
    // ANALYZE. On Oracle, approximate=false makes the driver run DBMS_STATS, which
    // throws ORA-20000 ("insufficient privileges") on tables the user can't analyze;
    // other dialects ignore this flag, so true is safe everywhere and cheaper.
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, true)) {
      while (rs.next()) {
        String indexName = rs.getString("INDEX_NAME");
        String col = rs.getString("COLUMN_NAME");
        if (indexName == null || col == null) {
          continue;
        }
        boolean unique = !rs.getBoolean("NON_UNIQUE");
        DatabaseTableIndexResponse idx =
            indexMap.computeIfAbsent(
                indexName, n -> new DatabaseTableIndexResponse(n, new ArrayList<>(), unique, null));
        idx.columns.add(col);
      }
    }
    List<DatabaseTableIndexResponse> indexes = new ArrayList<>(indexMap.values());

    // table identity + comment
    String schema = null;
    String tableType = null;
    String comment = null;
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] {"TABLE"})) {
      if (rs.next()) {
        schema = rs.getString("TABLE_SCHEM");
        tableType = rs.getString("TABLE_TYPE");
        comment = rs.getString("REMARKS");
      }
    }

    // row count + column count
    String qualified =
        (schema != null) ? "\"" + schema + "\".\"" + tableName + "\"" : "\"" + tableName + "\"";
    long rowCount = 0;
    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + qualified)) {
      rs.next();
      rowCount = rs.getLong(1);
    }

    int columnCount = 0;
    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        columnCount++;
      }
    }

    DatabaseTableInfoResponse info =
        new DatabaseTableInfoResponse(
            tableName, schema, tableType, comment, rowCount, columnCount, indexes.size());

    String createTableDdl = this.getCreateTableDdl(connection, tableName);

    return new DatabaseTableDetailResponse(info, indexes, dependencies, createTableDdl);
  }

  public String getCreateTableDdl(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();

    String schema = null;
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] {"TABLE"})) {
      if (rs.next()) {
        schema = rs.getString("TABLE_SCHEM");
      }
    }
    String qualified =
        (schema != null) ? "\"" + schema + "\".\"" + tableName + "\"" : "\"" + tableName + "\"";

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
      String cols =
          pkBySeq.values().stream().map(c -> "\"" + c + "\"").collect(Collectors.joining(", "));
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
