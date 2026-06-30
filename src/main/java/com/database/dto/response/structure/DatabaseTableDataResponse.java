package com.database.dto.response.structure;

import java.util.List;

public class DatabaseTableDataResponse {
  public List<DatabaseTableColumnMeta> columns;
  public List<List<Object>> rows;

  public DatabaseTableDataResponse(List<DatabaseTableColumnMeta> columns, List<List<Object>> rows) {
    this.columns = columns;
    this.rows = rows;
  }
}
