package com.database.dto.response.transfer;

import java.util.ArrayList;
import java.util.List;

/** Outcome of running an imported SQL script, one entry per statement. */
public class DatabaseImportResponse {

  public int totalStatements;
  public int succeeded;
  public int failed;
  public boolean stoppedOnError;
  public Long durationMs;

  public List<StatementResult> statements = new ArrayList<>();

  public DatabaseImportResponse() {}

  public void add(StatementResult result) {
    statements.add(result);
    if (result.success) {
      succeeded++;
    } else {
      failed++;
    }
    totalStatements++;
  }

  /** Result of a single statement within the script. */
  public static class StatementResult {

    public int index;
    public String statementPreview;
    public boolean success;
    public Integer affectedRows;

    public String errorMessage;
    public String sqlState;
    public Integer errorCode;

    private StatementResult() {}

    public static StatementResult ofSuccess(int index, String preview, int affectedRows) {
      StatementResult r = new StatementResult();
      r.index = index;
      r.statementPreview = preview;
      r.success = true;
      r.affectedRows = affectedRows;
      return r;
    }

    public static StatementResult ofError(
        int index, String preview, String errorMessage, String sqlState, int errorCode) {
      StatementResult r = new StatementResult();
      r.index = index;
      r.statementPreview = preview;
      r.success = false;
      r.errorMessage = errorMessage;
      r.sqlState = sqlState;
      r.errorCode = errorCode;
      return r;
    }
  }
}
