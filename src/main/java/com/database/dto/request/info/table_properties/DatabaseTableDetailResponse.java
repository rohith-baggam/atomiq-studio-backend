package com.database.dto.request.info.table_properties;

import com.database.dto.response.structure.DatabaseTableDependentTableResponse;
import java.util.List;

public class DatabaseTableDetailResponse {
  public DatabaseTableInfoResponse tableInfo;
  public List<DatabaseTableIndexResponse> indexes;
  public DatabaseTableDependentTableResponse dependencies;
  public String createTableDdl;

  public DatabaseTableDetailResponse(
      DatabaseTableInfoResponse tableInfo,
      List<DatabaseTableIndexResponse> indexes,
      DatabaseTableDependentTableResponse dependencies,
      String createTableDdl) {
    this.tableInfo = tableInfo;
    this.indexes = indexes;
    this.dependencies = dependencies;
    this.createTableDdl = createTableDdl;
  }
}
