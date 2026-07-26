package com.database.utils.structure;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

/**
 * Reads a single ResultSet cell into a value the JSON layer (Jackson) can serialize.
 *
 * <p>This runs against every dialect we support (MySQL, Postgres, MSSQL, Oracle), whose drivers
 * disagree on how columns are typed and what {@link ResultSet#getObject(int)} hands back. Three
 * categories need normalizing before they reach Jackson:
 *
 * <ul>
 *   <li><b>Temporal columns.</b> Oracle's driver returns proprietary {@code oracle.sql.TIMESTAMP}
 *       (and {@code TIMESTAMPTZ}/{@code TIMESTAMPLTZ}) instances, which Jackson cannot serialize.
 *       Asking the driver for the JDBC 4.2 {@code java.time} mapping gives every dialect the same
 *       serializable type. Postgres additionally reports {@code timestamptz} as {@link
 *       Types#TIMESTAMP} rather than {@link Types#TIMESTAMP_WITH_TIMEZONE}, so we disambiguate by
 *       the column type name.
 *   <li><b>Binary columns.</b> BLOB/RAW payloads are neither useful nor safe to inline in a JSON
 *       grid, so they collapse to a {@code "[binary]"} placeholder.
 *   <li><b>Everything else exotic.</b> JSON/JSONB, arrays, {@code uuid}, {@code inet}, {@code
 *       interval}, XML, spatial types, MSSQL {@code datetimeoffset}, driver-specific objects, and
 *       so on. These have no reliable {@code java.time}/boxed mapping, so we render them as text.
 * </ul>
 *
 * <p><b>Resilience contract:</b> reading a single cell must never abort the whole grid. Any
 * dialect/type quirk that makes the preferred conversion throw is caught and retried as {@link
 * ResultSet#getString(int)} — a rendering every driver supports. Only if that string fallback also
 * fails (a genuinely broken cursor or dead connection, which affects every row, not one cell) does
 * the error propagate to the caller's error handler.
 */
final class JdbcValueReader {

  private JdbcValueReader() {}

  static Object read(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
    try {
      return readTyped(rs, md, i);
    } catch (SQLException | RuntimeException conversionFailure) {
      // The driver could not produce the preferred type for this column (dialect quirk or an
      // exotic type we did not special-case). Degrade this ONE cell to text rather than failing
      // the entire response. If even getString cannot read it, the problem is not this cell —
      // let it propagate.
      return rs.getString(i);
    }
  }

  private static Object readTyped(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
    int type = md.getColumnType(i);
    switch (type) {
      case Types.BLOB:
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
        return rs.getObject(i) == null ? null : "[binary]";

        // Oracle DATE columns are reported as TIMESTAMP (mapDateToTimestamp=true), so they land
        // here and keep their time component.
      case Types.TIMESTAMP:
        // Postgres reports timestamptz as Types.TIMESTAMP (93) rather than
        // TIMESTAMP_WITH_TIMEZONE (2014), and its driver refuses to hand such a column back as a
        // LocalDateTime. Fall back to the type name to pick a compatible java.time type.
        if (isWithTimeZone(md.getColumnTypeName(i))) {
          return rs.getObject(i, OffsetDateTime.class);
        }
        return rs.getObject(i, LocalDateTime.class);
      case Types.TIMESTAMP_WITH_TIMEZONE:
        return rs.getObject(i, OffsetDateTime.class);
      case Types.DATE:
        return rs.getObject(i, LocalDate.class);
      case Types.TIME:
        return rs.getObject(i, LocalTime.class);
      case Types.TIME_WITH_TIMEZONE:
        return rs.getObject(i, OffsetTime.class);

        // Types with no dependable boxed/java.time mapping across drivers. getObject would hand
        // back driver-specific objects (PgArray, PGobject, oracle.sql.*, SQLXML, ...) that Jackson
        // chokes on, so render them as the driver's own text form. NULLs stay NULL.
      case Types.ARRAY:
      case Types.STRUCT:
      case Types.SQLXML:
      case Types.OTHER:
      case Types.JAVA_OBJECT:
      case Types.DATALINK:
      case Types.REF:
      case Types.ROWID:
        return rs.getString(i);

        // Character LOBs (Oracle NCLOB/CLOB, and the LONG variants). getObject hands back a driver
        // Clob/NClob proxy that Jackson cannot serialize — and because getObject SUCCEEDS, the
        // getString fallback in read() never fires, so an unhandled proxy would 500 the whole grid.
        // Materialize the character content here instead. (Any table with a Django TextField hits
        // this on Oracle.)
      case Types.CLOB:
      case Types.NCLOB:
      case Types.LONGVARCHAR:
      case Types.LONGNVARCHAR:
        return rs.getString(i);

      default:
        return rs.getObject(i);
    }
  }

  /**
   * True when the driver's column type name denotes a "with time zone" timestamp. Postgres uses
   * {@code timestamptz}; ANSI dialects spell it {@code timestamp with time zone}.
   */
  private static boolean isWithTimeZone(String columnTypeName) {
    if (columnTypeName == null) {
      return false;
    }
    String name = columnTypeName.toLowerCase();
    return name.contains("timestamptz") || name.contains("with time zone");
  }
}
