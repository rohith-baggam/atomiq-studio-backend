package com.database.dto.response.er_diagram;

public class DatabaseErDiagramTableFieldQuickResponse {

  public String fieldName;
  public String dataType;
  public boolean isPrimaryKey;
  public boolean isForeignKey;

  public DatabaseErDiagramTableFieldQuickResponse(
      String fieldName, String dataType, boolean isPrimaryKey, boolean isForeignKey) {
    this.fieldName = fieldName;
    this.dataType = dataType;
    this.isPrimaryKey = isPrimaryKey;
    this.isForeignKey = isForeignKey;
  }
}
