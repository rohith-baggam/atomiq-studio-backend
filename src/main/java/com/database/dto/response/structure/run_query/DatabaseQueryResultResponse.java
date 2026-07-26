package com.database.dto.response.structure.run_query;

import com.database.dto.response.structure.DatabaseTableColumnMeta;
import java.util.List;

public class DatabaseQueryResultResponse {

  public enum ResultType {
    ROWS,
    UPDATE,
    ERROR
  }

  public ResultType resultType;

  public List<DatabaseTableColumnMeta> columns;
  public List<List<Object>> rows;
  public Integer rowCount;
  public boolean truncated;

  public Integer affectedRows;
  public String message;

  public String errorMessage;
  public String sqlState;
  public Integer errorCode;

  public Long durationMs;

  private DatabaseQueryResultResponse() {}

  public static DatabaseQueryResultResponse ofRows(
      List<DatabaseTableColumnMeta> columns, List<List<Object>> rows, boolean truncated) {
    DatabaseQueryResultResponse r = new DatabaseQueryResultResponse();
    r.resultType = ResultType.ROWS;
    r.columns = columns;
    r.rows = rows;
    r.rowCount = rows.size();
    r.truncated = truncated;
    return r;
  }

  public static DatabaseQueryResultResponse ofUpdate(int affectedRows, String message) {
    DatabaseQueryResultResponse r = new DatabaseQueryResultResponse();
    r.resultType = ResultType.UPDATE;
    r.affectedRows = affectedRows;
    r.message = message;
    return r;
  }

  public static DatabaseQueryResultResponse ofError(
      String errorMessage, String sqlState, int errorCode) {
    DatabaseQueryResultResponse r = new DatabaseQueryResultResponse();
    r.resultType = ResultType.ERROR;
    r.errorMessage = errorMessage;
    r.sqlState = sqlState;
    r.errorCode = errorCode;
    return r;
  }
}
