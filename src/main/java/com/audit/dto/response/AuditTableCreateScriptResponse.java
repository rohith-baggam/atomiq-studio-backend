package com.audit.dto.response;

public class AuditTableCreateScriptResponse {

  public String originalSchemaScript;

  /** Strategy A: drift-proof JSONB payload. Null when the target dialect doesn't support it. */
  public String auditCreateScriptJsonb;

  /** Strategy B: twin-table with explicitly enumerated columns (no positional row expansion). */
  public String auditCreateScriptTwinTable;

  /** Echoes the request's chosen strategy (defaults to JSONB when the caller omits it). */
  public String selectedAuditPayloadStrategy;

  public AuditTableCreateScriptResponse(
      String originalSchemaScript,
      String auditCreateScriptJsonb,
      String auditCreateScriptTwinTable,
      String selectedAuditPayloadStrategy) {
    this.originalSchemaScript = originalSchemaScript;
    this.auditCreateScriptJsonb = auditCreateScriptJsonb;
    this.auditCreateScriptTwinTable = auditCreateScriptTwinTable;
    this.selectedAuditPayloadStrategy = selectedAuditPayloadStrategy;
  }
}
