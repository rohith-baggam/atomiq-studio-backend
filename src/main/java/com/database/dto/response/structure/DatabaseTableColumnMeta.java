package com.database.dto.response.structure;

public class DatabaseTableColumnMeta {
  public String columnName;
  public String dataType;

  public DatabaseTableColumnMeta(String columnName, String dataType) {
    this.columnName = columnName;
    this.dataType = dataType;
  }
}
