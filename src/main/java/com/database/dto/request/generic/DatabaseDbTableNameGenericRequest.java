package com.database.dto.request.generic;

import jakarta.ws.rs.QueryParam;

public class DatabaseDbTableNameGenericRequest {
  @QueryParam("tableName")
  public String tableName;
}
