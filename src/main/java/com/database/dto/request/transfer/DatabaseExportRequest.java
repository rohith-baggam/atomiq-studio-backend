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

  public List<String> tableNames;

  public DatabaseExportRequest() {}
}
