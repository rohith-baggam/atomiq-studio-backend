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
 * <p>Two kinds of driver values need normalizing before they reach Jackson:
 *
 * <ul>
 *   <li><b>Temporal columns.</b> Oracle's JDBC driver returns proprietary {@code
 *       oracle.sql.TIMESTAMP} (and {@code TIMESTAMPTZ}/{@code TIMESTAMPLTZ}) instances from {@link
 *       ResultSet#getObject(int)}. Jackson has no serializer for those, so the whole response fails
 *       with a 500 and an empty body. MySQL/Postgres already hand back {@code java.time}/{@code
 *       java.sql} types, which is why this only surfaced on Oracle. Asking the driver for the JDBC
 *       4.2 {@code java.time} mapping gives every dialect the same serializable type.
 *   <li><b>Binary columns.</b> BLOB/RAW payloads are neither useful nor safe to inline in a JSON
 *       grid, so they collapse to a {@code "[binary]"} placeholder.
 * </ul>
 */
final class JdbcValueReader {

  private JdbcValueReader() {}

  static Object read(ResultSet rs, ResultSetMetaData md, int i) throws SQLException {
    int type = md.getColumnType(i);
    switch (type) {
      case Types.BLOB:
      case Types.BINARY:
      case Types.VARBINARY:
      case Types.LONGVARBINARY:
        return rs.getObject(i) == null ? null : "[binary]";
        // Oracle DATE columns are reported as TIMESTAMP (mapDateToTimestamp=true), so they land
        // here
        // and keep their time component.
      case Types.TIMESTAMP:
        return rs.getObject(i, LocalDateTime.class);
      case Types.TIMESTAMP_WITH_TIMEZONE:
        return rs.getObject(i, OffsetDateTime.class);
      case Types.DATE:
        return rs.getObject(i, LocalDate.class);
      case Types.TIME:
        return rs.getObject(i, LocalTime.class);
      case Types.TIME_WITH_TIMEZONE:
        return rs.getObject(i, OffsetTime.class);
      default:
        return rs.getObject(i);
    }
  }
}
