package com.database.dto.request.info;

import jakarta.ws.rs.QueryParam;

public class DatabaseDbTableNameRequest {
  @QueryParam("tableName")
  public String tableName;
}
