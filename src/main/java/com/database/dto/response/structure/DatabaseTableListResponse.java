package com.database.dto.response.structure;

public class DatabaseTableListResponse {

  public String tableName;
  public long rowCount;

  public DatabaseTableListResponse(String tableName, long rowCount) {
    this.tableName = tableName;
    this.rowCount = rowCount;
  }
}
