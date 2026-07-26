package com.database.dto.response.er_diagram;

public class DatabaseErDiagramRelationResponse {

  public String fromTable;
  public String fromColumn;
  public String toTable;
  public String toColumn;

  public DatabaseErDiagramRelationResponse(
      String fromTable, String fromColumn, String toTable, String toColumn) {
    this.fromTable = fromTable;
    this.fromColumn = fromColumn;
    this.toTable = toTable;
    this.toColumn = toColumn;
  }
}
