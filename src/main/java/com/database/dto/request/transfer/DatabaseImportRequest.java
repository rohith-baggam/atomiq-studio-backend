package com.database.dto.request.transfer;

import jakarta.validation.constraints.NotBlank;

/**
 * A SQL script to execute against the connected database.
 *
 * <p>The script may contain many statements separated by semicolons (DDL and/or DML). When {@code
 * stopOnError} is true execution halts at the first failing statement; otherwise every statement is
 * attempted and per-statement errors are reported back in the response.
 */
public class DatabaseImportRequest {

  @NotBlank(message = "script is required")
  public String script;

  public boolean stopOnError = true;

  public DatabaseImportRequest() {}
}
