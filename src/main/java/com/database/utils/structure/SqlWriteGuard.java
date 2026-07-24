package com.database.utils.structure;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server-side backstop for read-only connections.
 *
 * <p>The frontend blocks write statements before calling run-query, but that gate is trivially
 * bypassable (it's a local HTTP API), so a read-only profile previously offered no real protection.
 * This mirrors that best-effort static check on the server so a profile flagged {@code readOnly}
 * genuinely cannot run DML/DDL. It is a keyword heuristic, not a full SQL parser — deliberately
 * conservative (it errs toward blocking).
 */
public final class SqlWriteGuard {

  private SqlWriteGuard() {}

  /** Leading keywords that write to the database (DML/DDL/admin). */
  private static final Set<String> WRITE_STARTERS =
      Set.of(
          "INSERT",
          "UPDATE",
          "DELETE",
          "MERGE",
          "UPSERT",
          "REPLACE",
          "CREATE",
          "ALTER",
          "DROP",
          "TRUNCATE",
          "RENAME",
          "GRANT",
          "REVOKE",
          "COMMENT",
          "CALL",
          "EXEC",
          "EXECUTE",
          "COPY",
          "IMPORT",
          "LOAD",
          "VACUUM",
          "REINDEX",
          "CLUSTER",
          "REFRESH");

  private static final Pattern LEADING_WORD = Pattern.compile("[a-zA-Z]+");
  private static final Pattern CTE_WRITE =
      Pattern.compile("\\b(INSERT|UPDATE|DELETE|MERGE)\\b", Pattern.CASE_INSENSITIVE);
  private static final Pattern INTO = Pattern.compile("\\bINTO\\b", Pattern.CASE_INSENSITIVE);

  /**
   * Returns the first write keyword found in {@code sql}, or {@code null} when it is pure reads.
   */
  public static String firstWriteKeyword(String sql) {
    if (sql == null) {
      return null;
    }
    String cleaned = stripNoise(sql);
    for (String raw : cleaned.split(";")) {
      String stmt = raw.trim();
      if (stmt.isEmpty()) {
        continue;
      }
      Matcher m = LEADING_WORD.matcher(stmt);
      if (!m.find()) {
        continue;
      }
      String first = m.group().toUpperCase();
      if (WRITE_STARTERS.contains(first)) {
        return first;
      }
      // Data-modifying CTE: WITH … INSERT/UPDATE/DELETE/MERGE …
      if (first.equals("WITH")) {
        Matcher c = CTE_WRITE.matcher(stmt);
        if (c.find()) {
          return c.group(1).toUpperCase();
        }
      }
      // SELECT … INTO new_table — creates a table.
      if (first.equals("SELECT") && INTO.matcher(stmt).find()) {
        return "SELECT INTO";
      }
    }
    return null;
  }

  /**
   * Strip comments and string/identifier literals so keyword matching isn't fooled by their text.
   */
  private static String stripNoise(String sql) {
    return sql.replaceAll("--[^\\n]*", " ")
        .replaceAll("/\\*[\\s\\S]*?\\*/", " ")
        .replaceAll("'(?:[^']|'')*'", "''")
        .replaceAll("\"(?:[^\"]|\"\")*\"", "\"\"");
  }
}
