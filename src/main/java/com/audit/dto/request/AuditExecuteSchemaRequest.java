package com.audit.dto.request;

import com.audit.utils.generate.AuditPayloadStrategy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public class AuditExecuteSchemaRequest {

  @NotBlank(message = "Table name is required")
  public String tableName;

  /** Which generated script to run; defaults to JSONB when omitted. */
  public AuditPayloadStrategy auditPayloadStrategy;

  /**
   * Explicit safety gate: the caller must acknowledge this creates real schema objects and a live
   * trigger.
   */
  @AssertTrue(message = "confirm must be true to execute the audit schema script")
  public boolean confirm;

  public AuditExecuteSchemaRequest() {}
}
