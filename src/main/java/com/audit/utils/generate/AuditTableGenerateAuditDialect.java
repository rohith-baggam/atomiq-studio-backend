package com.audit.utils.generate;

public interface AuditTableGenerateAuditDialect {

  /**
   * Quotes a single identifier (schema, table, function, trigger, or column name) so the emitted
   * SQL never depends on the engine's unquoted-identifier folding rules. Every identifier the
   * generator emits must be routed through this method - no bare concatenation of a name into
   * generated SQL.
   */
  String quoteIdent(String identifier);

  String renderType(AuditTableGenerateColumnMeta column);

  boolean supports(AuditPayloadStrategy strategy);

  /**
   * Ensures the audit schema/namespace exists before the twin table, function, or trigger try to be
   * created in it. Emitted first in the generated script so it is fully self-executable against a
   * clean database, not dependent on the audit schema having been created out-of-band.
   */
  String createAuditSchemaStatement();

  String buildTwinTable(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy);

  String buildTriggerFunction(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy);

  String buildTrigger(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy);
}
