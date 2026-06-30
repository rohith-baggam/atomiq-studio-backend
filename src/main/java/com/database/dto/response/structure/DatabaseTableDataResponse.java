package com.database.dto.response.structure;

import java.util.List;

public class DatabaseTableDataResponse {
  public List<DatabaseTableColumnMeta> columns;
  public List<List<Object>> rows;
  public boolean hasNext;
  public boolean hasPrev;
  public long count;

  public DatabaseTableDataResponse(
      List<DatabaseTableColumnMeta> columns,
      List<List<Object>> rows,
      boolean hasNext,
      boolean hasPrev,
      long count) {

    this.columns = columns;
    this.rows = rows;
    this.hasNext = hasNext;
    this.hasPrev = hasPrev;
    this.count = count;
  }
}
