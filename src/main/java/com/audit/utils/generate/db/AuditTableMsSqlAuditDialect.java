package com.audit.utils.generate.db;

import com.audit.utils.generate.AuditPayloadStrategy;
import com.audit.utils.generate.AuditTableAuditSchemaConfig;
import com.audit.utils.generate.AuditTableAuditTypeRenderer;
import com.audit.utils.generate.AuditTableGenerateAuditDialect;
import com.audit.utils.generate.AuditTableGenerateColumnMeta;
import com.audit.utils.generate.AuditTableGenerateTableSchema;
import java.util.stream.Collectors;

public final class AuditTableMsSqlAuditDialect implements AuditTableGenerateAuditDialect {

  @Override
  public String quoteIdent(String identifier) {
    return "[" + identifier.replace("]", "]]") + "]";
  }

  @Override
  public String renderType(AuditTableGenerateColumnMeta column) {
    return AuditTableAuditTypeRenderer.renderTypeDefault(column);
  }

  @Override
  public boolean supports(AuditPayloadStrategy strategy) {
    return strategy == AuditPayloadStrategy.TWIN_TABLE_EXPLICIT;
  }

  private void requireSupported(AuditPayloadStrategy strategy) {
    if (!supports(strategy)) {
      throw new UnsupportedOperationException(
          "Audit payload strategy " + strategy + " is not supported for MSSQL");
    }
  }

  private String auditSchema() {
    return quoteIdent(AuditTableAuditSchemaConfig.AUDIT_SCHEMA);
  }

  @Override
  public String createAuditSchemaStatement() {
    // MSSQL's CREATE SCHEMA has no IF NOT EXISTS; EXEC is required because CREATE SCHEMA
    // must be the only statement in its batch.
    String schemaLiteral = AuditTableAuditSchemaConfig.AUDIT_SCHEMA.replace("'", "''");
    return """
            IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'%1$s')
              EXEC('CREATE SCHEMA %2$s');
            """
        .formatted(schemaLiteral, auditSchema());
  }

  private String auditTable(AuditTableGenerateTableSchema schema) {
    return auditSchema() + "." + quoteIdent(schema.table() + "_audit");
  }

  private String sourceTable(AuditTableGenerateTableSchema schema) {
    String sourceSchema = schema.schema() != null ? schema.schema() : "dbo";
    return quoteIdent(sourceSchema) + "." + quoteIdent(schema.table());
  }

  @Override
  public String buildTwinTable(
      AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    String columnsDdl =
        schema.columns().stream()
            .map(c -> quoteIdent(c.name()) + " " + renderType(c))
            .collect(Collectors.joining(",\n  "));

    return """
            CREATE TABLE %1$s (
              audit_id        BIGINT IDENTITY(1,1) PRIMARY KEY,
              audit_operation CHAR(1) NOT NULL,
              audit_action_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
              audit_db_user   NVARCHAR(255) NOT NULL DEFAULT SUSER_SNAME(),
              audit_app_user  NVARCHAR(255),
              %2$s
            );
            CREATE INDEX %3$s ON %1$s (audit_action_at);
            """
        .formatted(
            auditTable(schema),
            columnsDdl,
            quoteIdent("ix_" + schema.table() + "_audit_action_at"));
  }

  @Override
  public String buildTriggerFunction(
      AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    return "-- MSSQL: trigger body is inline, see the CREATE TRIGGER statement below.";
  }

  @Override
  public String buildTrigger(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    String colList =
        schema.columns().stream().map(c -> quoteIdent(c.name())).collect(Collectors.joining(", "));

    return """
            CREATE TRIGGER %1$s
              ON %2$s
              AFTER INSERT, UPDATE, DELETE
            AS
            BEGIN
              SET NOCOUNT ON;

              IF EXISTS (SELECT 1 FROM inserted) AND EXISTS (SELECT 1 FROM deleted)
                INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                  SELECT 'U', CAST(SESSION_CONTEXT(N'app_user_id') AS NVARCHAR(255)), %4$s
                  FROM inserted;
              ELSE IF EXISTS (SELECT 1 FROM inserted)
                INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                  SELECT 'I', CAST(SESSION_CONTEXT(N'app_user_id') AS NVARCHAR(255)), %4$s
                  FROM inserted;
              ELSE IF EXISTS (SELECT 1 FROM deleted)
                INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                  SELECT 'D', CAST(SESSION_CONTEXT(N'app_user_id') AS NVARCHAR(255)), %4$s
                  FROM deleted;
            END;
            """
        .formatted(
            quoteIdent("trg_" + schema.table() + "_audit"),
            sourceTable(schema),
            auditTable(schema),
            colList);
  }
}
