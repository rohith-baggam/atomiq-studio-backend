package com.database.dto.response.structure;

import java.util.List;

public class DatabaseTableDependentTableResponse {
  public List<String> dependentOnTables;
  public List<String> dependentByTables;

  public DatabaseTableDependentTableResponse(
      List<String> dependentOnTables, List<String> dependentByTables) {
    this.dependentOnTables = dependentOnTables;
    this.dependentByTables = dependentByTables;
  }
}
