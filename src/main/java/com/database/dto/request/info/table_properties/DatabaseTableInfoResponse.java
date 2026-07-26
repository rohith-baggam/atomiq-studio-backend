package com.database.dto.request.info.table_properties;

public class DatabaseTableInfoResponse {
  public String tableName;
  public String schema;
  public String tableType;
  public String comment;
  public long rowCount;
  public int columnCount;
  public int indexCount;

  public DatabaseTableInfoResponse(
      String tableName,
      String schema,
      String tableType,
      String comment,
      long rowCount,
      int columnCount,
      int indexCount) {
    this.tableName = tableName;
    this.schema = schema;
    this.tableType = tableType;
    this.comment = comment;
    this.rowCount = rowCount;
    this.columnCount = columnCount;
    this.indexCount = indexCount;
  }
}
