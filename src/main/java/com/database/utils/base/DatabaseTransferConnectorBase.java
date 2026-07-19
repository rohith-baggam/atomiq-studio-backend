package com.database.utils.base;

import com.database.dto.response.transfer.DatabaseImportResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Dialect-aware JDBC connector for moving a whole database in and out as a portable SQL script.
 *
 * <p>Concrete subclasses (one per {@code DataBaseType}) supply only the pieces that genuinely
 * differ between engines — identifier quoting and boolean literal rendering — while the
 * export/import orchestration lives here so every dialect shares one, tested code path. This
 * mirrors the {@link DatabaseProfileLoginBase} abstract-connector pattern already used for
 * login/JDBC-url building.
 */
public abstract class DatabaseTransferConnectorBase {

  protected final Connection connection;

  protected DatabaseTransferConnectorBase(Connection connection) {
    this.connection = connection;
  }

  // --- dialect-specific hooks ------------------------------------------------

  /** Quote an identifier (schema/table/column) for this dialect. */
  public abstract String quoteIdentifier(String identifier);

  /** Human-readable dialect name, used in the export file header. */
  public abstract String dialectName();

  /** Render a boolean literal — Postgres accepts TRUE/FALSE, most others want 1/0. */
  protected String formatBoolean(boolean value) {
    return value ? "TRUE" : "FALSE";
  }

  // --- export ----------------------------------------------------------------

  /**
   * Serialise the connected database (optionally filtered to {@code tableFilter}) into a single SQL
   * script of CREATE TABLE statements followed, when {@code includeData} is set, by INSERT rows.
   */
  public String exportToSql(boolean includeData, List<String> tableFilter) throws SQLException {

    Set<String> wanted = normaliseFilter(tableFilter);

    StringBuilder sql = new StringBuilder();
    for (TableRef table : getUserTables()) {
      if (!wanted.isEmpty() && !wanted.contains(table.name.toLowerCase())) {
        continue;
      }
      sql.append(buildCreateTableDdl(table)).append("\n\n");
      if (includeData) {
        for (String insert : buildInserts(table)) {
          sql.append(insert).append("\n");
        }
        sql.append("\n");
      }
    }
    return sql.toString();
  }

  /** Number of user tables that would be exported under {@code tableFilter}. */
  public int countTables(List<String> tableFilter) throws SQLException {
    Set<String> wanted = normaliseFilter(tableFilter);
    int count = 0;
    for (TableRef table : getUserTables()) {
      if (wanted.isEmpty() || wanted.contains(table.name.toLowerCase())) {
        count++;
      }
    }
    return count;
  }

  protected String buildCreateTableDdl(TableRef table) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();
    String qualified = qualify(table);

    List<String> defs = new ArrayList<>();
    // Read columns in the JDBC spec's physical order; Oracle exposes COLUMN_DEF as a
    // forward-only stream, so reading it out of order throws ORA-17027.
    try (ResultSet rs = meta.getColumns(table.catalog, table.schema, table.name, "%")) {
      while (rs.next()) {
        String name = rs.getString("COLUMN_NAME");
        String type = rs.getString("TYPE_NAME");
        int size = rs.getInt("COLUMN_SIZE");
        int scale = rs.getInt("DECIMAL_DIGITS");
        boolean notNull = rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls;
        String columnDefault = rs.getString("COLUMN_DEF");

        StringBuilder col = new StringBuilder();
        col.append("  ")
            .append(quoteIdentifier(name))
            .append(" ")
            .append(renderType(type, size, scale));
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
    try (ResultSet rs = meta.getPrimaryKeys(table.catalog, table.schema, table.name)) {
      while (rs.next()) {
        pkBySeq.put(rs.getShort("KEY_SEQ"), rs.getString("COLUMN_NAME"));
      }
    }
    if (!pkBySeq.isEmpty()) {
      String cols =
          pkBySeq.values().stream().map(this::quoteIdentifier).collect(Collectors.joining(", "));
      defs.add("  PRIMARY KEY (" + cols + ")");
    }

    return "CREATE TABLE " + qualified + " (\n" + String.join(",\n", defs) + "\n);";
  }

  protected List<String> buildInserts(TableRef table) throws SQLException {
    String qualified = qualify(table);
    List<String> inserts = new ArrayList<>();

    try (Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT * FROM " + qualified)) {

      ResultSetMetaData md = rs.getMetaData();
      int colCount = md.getColumnCount();

      List<String> colNames = new ArrayList<>(colCount);
      for (int i = 1; i <= colCount; i++) {
        colNames.add(quoteIdentifier(md.getColumnLabel(i)));
      }
      String colList = String.join(", ", colNames);

      while (rs.next()) {
        List<String> values = new ArrayList<>(colCount);
        for (int i = 1; i <= colCount; i++) {
          values.add(formatValue(rs.getObject(i)));
        }
        inserts.add(
            "INSERT INTO "
                + qualified
                + " ("
                + colList
                + ") VALUES ("
                + String.join(", ", values)
                + ");");
      }
    }
    return inserts;
  }

  /** Render a Java value as a SQL literal. Binary values are not portable and become NULL. */
  protected String formatValue(Object value) {
    if (value == null) {
      return "NULL";
    }
    if (value instanceof Boolean) {
      return formatBoolean((Boolean) value);
    }
    if (value instanceof Number) {
      return value.toString();
    }
    if (value instanceof byte[]) {
      return "NULL";
    }
    return "'" + value.toString().replace("'", "''") + "'";
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

  protected List<TableRef> getUserTables() throws SQLException {
    List<TableRef> tables = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String schema = rs.getString("TABLE_SCHEM");
        if (isSystemSchema(schema)) {
          continue;
        }
        tables.add(new TableRef(rs.getString("TABLE_CAT"), schema, rs.getString("TABLE_NAME")));
      }
    }
    return tables;
  }

  private boolean isSystemSchema(String schema) {
    if (schema == null) {
      return false;
    }
    String s = schema.toLowerCase();
    if (s.startsWith("pg_")) {
      return true;
    }
    Set<String> system =
        Set.of("information_schema", "sys", "mysql", "performance_schema", "sysaux");
    return system.contains(s);
  }

  protected String qualify(TableRef table) {
    if (table.schema != null && !table.schema.isBlank()) {
      return quoteIdentifier(table.schema) + "." + quoteIdentifier(table.name);
    }
    return quoteIdentifier(table.name);
  }

  private Set<String> normaliseFilter(List<String> tableFilter) {
    Set<String> wanted = new LinkedHashSet<>();
    if (tableFilter != null) {
      for (String name : tableFilter) {
        if (name != null && !name.isBlank()) {
          wanted.add(name.trim().toLowerCase());
        }
      }
    }
    return wanted;
  }

  // --- import ----------------------------------------------------------------

  /**
   * Execute a SQL script statement by statement, collecting a per-statement outcome. When {@code
   * stopOnError} is set the first failure halts execution; otherwise every statement is attempted.
   */
  public DatabaseImportResponse importFromSql(String script, boolean stopOnError)
      throws SQLException {

    DatabaseImportResponse response = new DatabaseImportResponse();
    long start = System.nanoTime();

    List<String> statements = splitStatements(script);
    int index = 0;
    for (String statement : statements) {
      index++;
      String preview = preview(statement);
      try (Statement st = connection.createStatement()) {
        boolean isResultSet = st.execute(statement);
        int affected = isResultSet ? 0 : st.getUpdateCount();
        response.add(
            DatabaseImportResponse.StatementResult.ofSuccess(
                index, preview, Math.max(affected, 0)));
      } catch (SQLException e) {
        response.add(
            DatabaseImportResponse.StatementResult.ofError(
                index, preview, e.getMessage(), e.getSQLState(), e.getErrorCode()));
        if (stopOnError) {
          response.stoppedOnError = true;
          break;
        }
      }
    }

    response.durationMs = (System.nanoTime() - start) / 1_000_000;
    return response;
  }

  private String preview(String statement) {
    String flat = statement.replaceAll("\\s+", " ").trim();
    return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
  }

  /**
   * Split a script into individual statements on top-level semicolons, while respecting
   * single/double quoted strings, backtick identifiers, line comments ({@code --}, {@code #}) and
   * block comments.
   */
  protected List<String> splitStatements(String script) {
    List<String> statements = new ArrayList<>();
    StringBuilder current = new StringBuilder();

    boolean inSingle = false;
    boolean inDouble = false;
    boolean inBacktick = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;

    int length = script.length();
    for (int i = 0; i < length; i++) {
      char c = script.charAt(i);
      char next = (i + 1 < length) ? script.charAt(i + 1) : '\0';

      if (inLineComment) {
        if (c == '\n') {
          inLineComment = false;
          current.append(c);
        }
        continue;
      }
      if (inBlockComment) {
        if (c == '*' && next == '/') {
          inBlockComment = false;
          i++;
        }
        continue;
      }

      if (!inSingle && !inDouble && !inBacktick) {
        if (c == '-' && next == '-') {
          inLineComment = true;
          i++;
          continue;
        }
        if (c == '#') {
          inLineComment = true;
          continue;
        }
        if (c == '/' && next == '*') {
          inBlockComment = true;
          i++;
          continue;
        }
      }

      if (c == '\'' && !inDouble && !inBacktick) {
        inSingle = !inSingle;
      } else if (c == '"' && !inSingle && !inBacktick) {
        inDouble = !inDouble;
      } else if (c == '`' && !inSingle && !inDouble) {
        inBacktick = !inBacktick;
      }

      if (c == ';' && !inSingle && !inDouble && !inBacktick) {
        String statement = current.toString().trim();
        if (!statement.isEmpty()) {
          statements.add(statement);
        }
        current.setLength(0);
        continue;
      }

      current.append(c);
    }

    String tail = current.toString().trim();
    if (!tail.isEmpty()) {
      statements.add(tail);
    }
    return statements;
  }

  /** Catalog/schema/name triple identifying one table in a dialect-neutral way. */
  protected static class TableRef {
    public final String catalog;
    public final String schema;
    public final String name;

    public TableRef(String catalog, String schema, String name) {
      this.catalog = catalog;
      this.schema = schema;
      this.name = name;
    }
  }
}
