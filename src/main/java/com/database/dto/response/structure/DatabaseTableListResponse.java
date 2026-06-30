package com.database.dto.response.structure;

public class DatabaseTableListResponse {

  public String tableName;
  public long rowCount;
  public long columnCount;

  public DatabaseTableListResponse(String tableName, long rowCount, long columnCount) {
    this.tableName = tableName;
    this.rowCount = rowCount;
    this.columnCount = columnCount;
  }
}
