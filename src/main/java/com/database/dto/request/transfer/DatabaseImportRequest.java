package com.database.dto.request.transfer;

/**
 * A SQL script to execute against the connected database.
 *
 * <p>The script may contain many statements separated by semicolons (DDL and/or DML). When {@code
 * stopOnError} is true execution halts at the first failing statement; otherwise every statement is
 * attempted and per-statement errors are reported back in the response.
 *
 * <p>Provide the script one of two ways: inline via {@code script} (small scripts), or via {@code
 * filePath} — an absolute path on the backend machine that is read and executed straight off disk
 * with bounded memory, which is what keeps a multi-GB {@code .sql} import from OOM'ing. Exactly one
 * is required; when both are present {@code filePath} wins.
 */
public class DatabaseImportRequest {

  public String script;

  /** Absolute path to a {@code .sql} file to stream and execute; alternative to {@code script}. */
  public String filePath;

  public boolean stopOnError = true;

  public DatabaseImportRequest() {}
}
