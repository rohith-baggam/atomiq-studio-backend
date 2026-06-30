package com.database.dto.request.info;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

public class DatabaseDbTableNameRequest {
  @QueryParam("tableName")
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
