package com.database.dto.request.transfer;

import java.util.List;

/**
 * Options for exporting the connected database to a portable SQL script.
 *
 * <p>{@code includeData} toggles whether INSERT statements are emitted alongside the CREATE TABLE
 * DDL. {@code tableNames} optionally restricts the export to a subset of tables — when null or
 * empty every user table is exported.
 */
public class DatabaseExportRequest {

  public boolean includeData = true;

  /**
   * Whether CREATE TABLE DDL is emitted. Together with {@code includeData} this expresses all three
   * export modes: schema-only ({@code includeSchema} true, {@code includeData} false), data-only
   * (false/true) and schema+data (true/true). Data-only used to be derived by stripping DDL in the
   * browser, which is impossible when the dump streams straight to disk — so the choice is made
   * here.
   */
  public boolean includeSchema = true;

  public List<String> tableNames;

  /**
   * Optional absolute path (on the machine running the backend) to stream the dump to. When set,
   * the backend writes the SQL straight to this file with bounded memory and returns metadata only
   * — the memory-safe path for multi-GB databases. When null the dump is returned inline (bounded
   * to a preview sample), which is what the browser/dev build uses.
   */
  public String filePath;

  public DatabaseExportRequest() {}
}
