package com.database.dto.request.info;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class DatabaseDbTableDataRequest {
  @QueryParam("tableName")
  @NotBlank(message = "Table name is required field")
  public String tableName;

  @QueryParam("limit")
  @DefaultValue("10")
  public Integer limit;

  @QueryParam("offset")
  @DefaultValue("0")
  public Integer offset;

  @QueryParam("sortBy")
  public String sortBy;

  @QueryParam("sortDir")
  public String sortDir;
}
