package com.audit.dto.response;

public class AuditTableExecuteScriptResponse {

  public String tableName;
  public String appliedAuditPayloadStrategy;
  public String executedScript;
  public String message;

  public AuditTableExecuteScriptResponse(
      String tableName, String appliedAuditPayloadStrategy, String executedScript, String message) {
    this.tableName = tableName;
    this.appliedAuditPayloadStrategy = appliedAuditPayloadStrategy;
    this.executedScript = executedScript;
    this.message = message;
  }
}
