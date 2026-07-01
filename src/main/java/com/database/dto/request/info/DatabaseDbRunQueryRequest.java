package com.database.dto.request.info;

import jakarta.validation.constraints.NotBlank;

public class DatabaseDbRunQueryRequest {

  @NotBlank(message = "query is required")
  public String query;

  public Integer maxRows;
  public Integer timeoutSeconds;

  public DatabaseDbRunQueryRequest() {}
}
