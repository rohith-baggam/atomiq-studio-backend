package com.database.dto.response.transfer;

/** The generated SQL dump plus metadata describing it. */
public class DatabaseExportResponse {

  public String fileName;
  public String sql;
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
