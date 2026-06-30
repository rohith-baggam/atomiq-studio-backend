package com.database.dto.response.info;

public class DatabaseTableFieldsHelperListResponse {

  public String columnName;
  public String dataType;

  public DatabaseTableFieldsHelperListResponse(String columnName, String dataType) {
    this.columnName = columnName;
    this.dataType = dataType;
  }
}
