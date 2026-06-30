package com.database.dto.response.er_diagram;

import java.util.List;

public class DatabaseErDiagramTableResponse {

  public String tableName;
  public List<DatabaseErDiagramTableFieldQuickResponse> fieldQuickResponses;

  public DatabaseErDiagramTableResponse(
      String tableName, List<DatabaseErDiagramTableFieldQuickResponse> fieldQuickResponses) {
    this.tableName = tableName;
    this.fieldQuickResponses = fieldQuickResponses;
  }
}
