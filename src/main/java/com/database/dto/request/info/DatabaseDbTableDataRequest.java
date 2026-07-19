package com.database.dto.request.info;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.List;

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

  /** Global text search — case-insensitive "contains" across every column. */
  @QueryParam("search")
  public String search;

  /* Per-column filters, sent as three parallel lists so the i-th column, op and
   * value form one filter (fCol[i] <fOp[i]> fVal[i]). Column names are validated
   * against the table's real columns and values are bound as prepared-statement
   * parameters — never string-concatenated into SQL. */
  @QueryParam("fCol")
  public List<String> filterColumns;

  @QueryParam("fOp")
  public List<String> filterOps;

  @QueryParam("fVal")
  public List<String> filterValues;
}
