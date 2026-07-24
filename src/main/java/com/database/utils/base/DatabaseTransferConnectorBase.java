package com.database.utils.base;

import com.database.dto.response.transfer.DatabaseImportResponse;
import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
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

  /**
   * JDBC fetch size for the row-by-row export cursor. The default (1000) tells most drivers to hand
   * rows over in batches instead of buffering a whole table client-side. MySQL is the exception —
   * see {@code DatabaseMySqlTransfer}.
   */
  protected int streamingFetchSize() {
    return 1000;
  }

  /** Outcome of a streaming export: how many tables were written, and whether rows were capped. */
  public static final class ExportStats {
    public final int tableCount;
    public final boolean truncated;

    public ExportStats(int tableCount, boolean truncated) {
      this.tableCount = tableCount;
      this.truncated = truncated;
    }
  }

  // --- export ----------------------------------------------------------------

  /**
   * Stream the connected database (optionally filtered to {@code tableFilter}) straight to {@code
   * out} as a portable SQL script — CREATE TABLE DDL followed, when {@code includeData} is set, by
   * INSERT rows. Rows are read through a server-side cursor and written one at a time, so a
   * multi-GB table never has to be held in memory (the previous String-building approach OOM'd on
   * large databases). {@code maxRowsPerTable <= 0} means unlimited (a real export); a positive cap
   * bounds the inline preview. Returns table count and whether any table's rows were capped.
   */
  public ExportStats exportToWriter(
      Writer out,
      boolean includeSchema,
      boolean includeData,
      List<String> tableFilter,
      int maxRowsPerTable)
      throws SQLException, IOException {

    Set<String> wanted = normaliseFilter(tableFilter);

    // A server-side cursor needs auto-commit off on some drivers (notably Postgres, which
    // otherwise buffers the entire ResultSet). Export owns the connection for its duration, so
    // toggling it here is safe; restore it afterwards.
    boolean priorAutoCommit = connection.getAutoCommit();
    boolean toggledAutoCommit = false;
    if (includeData && priorAutoCommit) {
      connection.setAutoCommit(false);
      toggledAutoCommit = true;
    }

    int tableCount = 0;
    boolean truncated = false;
    try {
      for (TableRef table : getUserTables()) {
        if (!wanted.isEmpty() && !wanted.contains(table.name.toLowerCase())) {
          continue;
        }
        if (includeSchema) {
          out.write(buildCreateTableDdl(table));
          out.write("\n\n");
        }
        if (includeData) {
          truncated |= writeInserts(out, table, maxRowsPerTable);
          out.write("\n");
        }
        tableCount++;
      }
    } finally {
      if (toggledAutoCommit) {
        connection.setAutoCommit(priorAutoCommit);
      }
    }
    return new ExportStats(tableCount, truncated);
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

  /**
   * Stream one INSERT per row of {@code table} directly to {@code out}. Only a single row's worth
   * of text is materialised at a time. Returns true when the row cap ({@code maxRows > 0}) stopped
   * it early, so the caller can flag the export/preview as truncated.
   */
  protected boolean writeInserts(Writer out, TableRef table, int maxRows)
      throws SQLException, IOException {
    String qualified = qualify(table);

    try (Statement st = connection.createStatement()) {
      st.setFetchSize(streamingFetchSize());
      try (ResultSet rs = st.executeQuery("SELECT * FROM " + qualified)) {

        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();

        List<String> colNames = new ArrayList<>(colCount);
        for (int i = 1; i <= colCount; i++) {
          colNames.add(quoteIdentifier(md.getColumnLabel(i)));
        }
        String colList = String.join(", ", colNames);
        String prefix = "INSERT INTO " + qualified + " (" + colList + ") VALUES (";

        long written = 0;
        while (rs.next()) {
          if (maxRows > 0 && written >= maxRows) {
            return true;
          }
          StringBuilder row = new StringBuilder(prefix);
          for (int i = 1; i <= colCount; i++) {
            if (i > 1) {
              row.append(", ");
            }
            row.append(readCellLiteral(rs, md, i));
          }
          row.append(");\n");
          out.write(row.toString());
          written++;
        }
      }
    }
    return false;
  }

  /**
   * Render one result-set cell as a SQL literal, robustly. Some driver values must not go through
   * {@code getObject}: Oracle {@code XMLType} needs the optional {@code xdb} jars and throws {@link
   * NoClassDefFoundError} without them; LOBs come back as {@code Blob}/{@code Clob} handles whose
   * {@code toString} is useless; binary isn't portable. So we read text-like large types as
   * strings, drop binary/opaque ones to {@code NULL}, and — crucially — never let one odd column
   * abort the whole export (we catch {@link Throwable}, since the driver failures here are {@code
   * Error}s).
   */
  protected String readCellLiteral(ResultSet rs, ResultSetMetaData md, int i) {
    int type;
    try {
      type = md.getColumnType(i);
    } catch (SQLException e) {
      type = java.sql.Types.OTHER;
    }
    try {
      switch (type) {
        case java.sql.Types.BLOB:
        case java.sql.Types.BINARY:
        case java.sql.Types.VARBINARY:
        case java.sql.Types.LONGVARBINARY:
          return "NULL";
        case java.sql.Types.CLOB:
        case java.sql.Types.NCLOB:
        case java.sql.Types.SQLXML:
        case java.sql.Types.LONGVARCHAR:
        case java.sql.Types.LONGNVARCHAR:
          return stringLiteral(rs.getString(i));
        case java.sql.Types.STRUCT:
        case java.sql.Types.JAVA_OBJECT:
        case java.sql.Types.OTHER:
        case java.sql.Types.REF:
        case java.sql.Types.ARRAY:
          // Opaque/structured (incl. Oracle XMLType, spatial, UDTs): try text, else NULL.
          try {
            return stringLiteral(rs.getString(i));
          } catch (Throwable t) {
            return "NULL";
          }
        default:
          return formatValue(rs.getObject(i));
      }
    } catch (Throwable t) {
      // e.g. a driver needing an optional jar (Oracle xdb). Fall back rather than 500 the export.
      try {
        return stringLiteral(rs.getString(i));
      } catch (Throwable t2) {
        return "NULL";
      }
    }
  }

  private String stringLiteral(String value) {
    return value == null ? "NULL" : "'" + value.replace("'", "''") + "'";
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

    // Oracle equates schemas with users, so getTables() returns tables from every
    // Oracle-maintained system schema (SYS, XDB, MDSYS, ...) the connected user can
    // see — that's why an "entire DB" export was pulling in dozens of tables the
    // table-list view (which filters ORACLE_MAINTAINED='N') never shows. Restrict
    // to user-created schemas the same way, so the export scope matches the UI.
    Set<String> oracleUserSchemas = null;
    String product = meta.getDatabaseProductName();
    if (product != null && product.toLowerCase().contains("oracle")) {
      oracleUserSchemas = new HashSet<>();
      try (Statement st = connection.createStatement();
          ResultSet rs =
              st.executeQuery("SELECT USERNAME FROM ALL_USERS WHERE ORACLE_MAINTAINED = 'N'")) {
        while (rs.next()) {
          oracleUserSchemas.add(rs.getString("USERNAME"));
        }
      }
    }

    try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String schema = rs.getString("TABLE_SCHEM");
        if (isSystemSchema(schema)) {
          continue;
        }
        if (oracleUserSchemas != null && !oracleUserSchemas.contains(schema)) {
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
   * Execute a SQL script (in memory) statement by statement. Thin wrapper over {@link
   * #importFromReader} for the small-script case where the whole thing is already a String.
   */
  public DatabaseImportResponse importFromSql(String script, boolean stopOnError)
      throws SQLException {
    return importFromReader(new StringReader(script), stopOnError);
  }

  /**
   * Execute a SQL script read from {@code reader} statement by statement, collecting a
   * per-statement outcome. Statements are split and run as they stream off the reader, so a
   * multi-GB {@code .sql} file is never held in memory (it can be a {@link java.io.BufferedReader}
   * straight off disk).
   *
   * <p>When {@code stopOnError} is set the first failure halts execution AND the whole run is one
   * transaction that rolls back on failure, so the target database is never left half-imported.
   * Otherwise every statement is attempted independently (auto-commit) and per-statement errors are
   * reported.
   */
  public DatabaseImportResponse importFromReader(Reader reader, boolean stopOnError)
      throws SQLException {

    DatabaseImportResponse response = new DatabaseImportResponse();
    long start = System.nanoTime();

    boolean transactional = stopOnError;
    boolean priorAutoCommit = connection.getAutoCommit();
    if (transactional) {
      connection.setAutoCommit(false);
    }

    int[] index = {0};
    try {
      streamStatements(
          reader,
          statement -> {
            index[0]++;
            String preview = preview(statement);
            try (Statement st = connection.createStatement()) {
              boolean isResultSet = st.execute(statement);
              int affected = isResultSet ? 0 : st.getUpdateCount();
              response.add(
                  DatabaseImportResponse.StatementResult.ofSuccess(
                      index[0], preview, Math.max(affected, 0)));
              return true;
            } catch (SQLException e) {
              response.add(
                  DatabaseImportResponse.StatementResult.ofError(
                      index[0], preview, e.getMessage(), e.getSQLState(), e.getErrorCode()));
              if (stopOnError) {
                response.stoppedOnError = true;
                return false; // stop streaming
              }
              return true;
            }
          });
      if (transactional) {
        if (response.stoppedOnError) {
          connection.rollback();
        } else {
          connection.commit();
        }
      }
    } catch (IOException e) {
      throw new SQLException("Failed reading import script: " + e.getMessage(), e);
    } finally {
      if (transactional) {
        connection.setAutoCommit(priorAutoCommit);
      }
    }

    response.durationMs = (System.nanoTime() - start) / 1_000_000;
    return response;
  }

  private String preview(String statement) {
    String flat = statement.replaceAll("\\s+", " ").trim();
    return flat.length() > 120 ? flat.substring(0, 120) + "…" : flat;
  }

  /** Consumes one complete statement; returns {@code false} to stop the stream early. */
  @FunctionalInterface
  protected interface StatementConsumer {
    boolean accept(String statement) throws SQLException;
  }

  /**
   * Split {@code reader} into individual statements on top-level semicolons — respecting
   * single/double quoted strings, backtick identifiers, line comments ({@code --}, {@code #}) and
   * block comments — feeding each complete statement to {@code consumer} as soon as it is seen.
   * Only the current statement is buffered, so the source can be arbitrarily large. Stops early if
   * the consumer returns false.
   */
  protected void streamStatements(Reader in, StatementConsumer consumer)
      throws IOException, SQLException {
    PushbackReader reader = new PushbackReader(in, 1);
    StringBuilder current = new StringBuilder();

    boolean inSingle = false;
    boolean inDouble = false;
    boolean inBacktick = false;
    boolean inLineComment = false;
    boolean inBlockComment = false;

    int r;
    while ((r = reader.read()) != -1) {
      char c = (char) r;
      char next = peek(reader);

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
          reader.read(); // consume '/'
        }
        continue;
      }

      if (!inSingle && !inDouble && !inBacktick) {
        if (c == '-' && next == '-') {
          inLineComment = true;
          reader.read(); // consume second '-'
          continue;
        }
        if (c == '#') {
          inLineComment = true;
          continue;
        }
        if (c == '/' && next == '*') {
          inBlockComment = true;
          reader.read(); // consume '*'
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
        current.setLength(0);
        if (!statement.isEmpty() && !consumer.accept(statement)) {
          return;
        }
        continue;
      }

      current.append(c);
    }

    String tail = current.toString().trim();
    if (!tail.isEmpty()) {
      consumer.accept(tail);
    }
  }

  /** Read the next char without consuming it (returns '\0' at end of stream). */
  private char peek(PushbackReader reader) throws IOException {
    int n = reader.read();
    if (n == -1) {
      return '\0';
    }
    reader.unread(n);
    return (char) n;
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
