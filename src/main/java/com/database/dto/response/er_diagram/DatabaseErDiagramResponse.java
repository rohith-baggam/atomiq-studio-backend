package com.database.dto.response.er_diagram;

import java.util.List;

public class DatabaseErDiagramResponse {

  public List<DatabaseErDiagramTableResponse> tables;
  public List<DatabaseErDiagramRelationResponse> relationships;

  public DatabaseErDiagramResponse(
      List<DatabaseErDiagramTableResponse> tables,
      List<DatabaseErDiagramRelationResponse> relationships) {
    this.tables = tables;
    this.relationships = relationships;
  }
}
