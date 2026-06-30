package com.database.dto.request.info.table_properties;

import java.util.List;

public class DatabaseTableIndexResponse {
  public String indexName;
  public List<String> columns;
  public boolean unique;
  public String method;

  public DatabaseTableIndexResponse(
      String indexName, List<String> columns, boolean unique, String method) {
    this.indexName = indexName;
    this.columns = columns;
    this.unique = unique;
    this.method = method;
  }
}
