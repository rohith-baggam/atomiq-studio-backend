package com.database.dto.request.generic;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.QueryParam;

public class DatabaseDbTableNameGenericRequest {
  @QueryParam("tableName")
  @NotBlank(message = "Table name is required")
  public String tableName;
}
