package com.database.dto.response.transfer;

/** The generated SQL dump plus metadata describing it. */
public class DatabaseExportResponse {

  public String fileName;

  /** The dump text — populated for an inline export; null when streamed to {@link #filePath}. */
  public String sql;

  /** Where the dump was written, when the request asked to stream to disk; null for inline. */
  public String filePath;

  /** Size of the written file in bytes, when streamed to {@link #filePath}. */
  public Long byteCount;

  /** True when an inline preview was capped to a per-table sample (large database). */
  public boolean truncated;

  public int tableCount;
  public boolean includeData;
  public String generatedAt;

  public DatabaseExportResponse() {}

  public DatabaseExportResponse(
      String fileName, String sql, int tableCount, boolean includeData, String generatedAt) {
    this.fileName = fileName;
    this.sql = sql;
    this.tableCount = tableCount;
    this.includeData = includeData;
    this.generatedAt = generatedAt;
  }
}
