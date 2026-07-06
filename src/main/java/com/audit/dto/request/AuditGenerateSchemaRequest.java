package com.audit.dto.request;

import com.audit.utils.generate.AuditPayloadStrategy;
import jakarta.validation.constraints.NotBlank;

public class AuditGenerateSchemaRequest {

  @NotBlank(message = "Table name is required")
  public String tableName;

  /** Caller's preferred trigger payload strategy; defaults to JSONB when omitted. */
  public AuditPayloadStrategy auditPayloadStrategy;

  public AuditGenerateSchemaRequest() {}
}
