package com.audit.utils.generate.db;

import com.audit.utils.generate.AuditPayloadStrategy;
import com.audit.utils.generate.AuditTableAuditSchemaConfig;
import com.audit.utils.generate.AuditTableAuditTypeRenderer;
import com.audit.utils.generate.AuditTableGenerateAuditDialect;
import com.audit.utils.generate.AuditTableGenerateColumnMeta;
import com.audit.utils.generate.AuditTableGenerateTableSchema;
import java.util.List;
import java.util.stream.Collectors;

public final class AuditTablePostgresAuditDialect implements AuditTableGenerateAuditDialect {

  @Override
  public String quoteIdent(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String renderType(AuditTableGenerateColumnMeta column) {
    return AuditTableAuditTypeRenderer.renderTypeDefault(column);
  }

  @Override
  public boolean supports(AuditPayloadStrategy strategy) {
    return true;
  }

  private String auditSchema() {
    return quoteIdent(AuditTableAuditSchemaConfig.AUDIT_SCHEMA);
  }

  @Override
  public String createAuditSchemaStatement() {
    return "CREATE SCHEMA IF NOT EXISTS " + auditSchema() + ";";
  }

  private String auditTable(AuditTableGenerateTableSchema schema) {
    return auditSchema() + "." + quoteIdent(schema.table() + "_audit");
  }

  private String fnName(AuditTableGenerateTableSchema schema) {
    return auditSchema() + "." + quoteIdent("fn_" + schema.table() + "_audit");
  }

  private String triggerName(AuditTableGenerateTableSchema schema) {
    return quoteIdent("trg_" + schema.table() + "_audit");
  }

  private String sourceTable(AuditTableGenerateTableSchema schema) {
    String sourceSchema = schema.schema() != null ? schema.schema() : "public";
    return quoteIdent(sourceSchema) + "." + quoteIdent(schema.table());
  }

  @Override
  public String buildTwinTable(
      AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    String mirroredColumnsDdl =
        switch (strategy) {
          case JSONB -> "  audit_row       JSONB NOT NULL";
          case TWIN_TABLE_EXPLICIT ->
              schema.columns().stream()
                  .map(c -> "  " + quoteIdent(c.name()) + " " + renderType(c))
                  .collect(Collectors.joining(",\n"));
        };

    return """
            CREATE TABLE %1$s (
              audit_id        BIGSERIAL PRIMARY KEY,
              audit_operation CHAR(1) NOT NULL,
              audit_action_at TIMESTAMPTZ NOT NULL DEFAULT now(),
              audit_db_user   TEXT NOT NULL DEFAULT current_user,
              audit_app_user  TEXT,
            %2$s
            );
            CREATE INDEX ON %1$s (audit_action_at);
            """
        .formatted(auditTable(schema), mirroredColumnsDdl);
  }

  @Override
  public String buildTriggerFunction(
      AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    String deleteInsert;
    String upsertInsert;
    switch (strategy) {
      case JSONB -> {
        deleteInsert =
            """
                    INSERT INTO %1$s (audit_operation, audit_app_user, audit_row)
                          VALUES ('D', current_setting('app.user_id', true), to_jsonb(OLD));"""
                .formatted(auditTable(schema));
        upsertInsert =
            """
                    INSERT INTO %1$s (audit_operation, audit_app_user, audit_row)
                          VALUES (LEFT(TG_OP, 1), current_setting('app.user_id', true), to_jsonb(NEW));"""
                .formatted(auditTable(schema));
      }
      case TWIN_TABLE_EXPLICIT -> {
        List<String> names =
            schema.columns().stream().map(AuditTableGenerateColumnMeta::name).toList();
        String insertCols = names.stream().map(this::quoteIdent).collect(Collectors.joining(", "));
        String oldVals =
            names.stream().map(n -> "(OLD)." + quoteIdent(n)).collect(Collectors.joining(", "));
        String newVals =
            names.stream().map(n -> "(NEW)." + quoteIdent(n)).collect(Collectors.joining(", "));

        deleteInsert =
            """
                    INSERT INTO %1$s (audit_operation, audit_app_user, %2$s)
                          VALUES ('D', current_setting('app.user_id', true), %3$s);"""
                .formatted(auditTable(schema), insertCols, oldVals);
        upsertInsert =
            """
                    INSERT INTO %1$s (audit_operation, audit_app_user, %2$s)
                          VALUES (LEFT(TG_OP, 1), current_setting('app.user_id', true), %3$s);"""
                .formatted(auditTable(schema), insertCols, newVals);
      }
      default -> throw new IllegalArgumentException("Unsupported strategy: " + strategy);
    }

    return """
            CREATE OR REPLACE FUNCTION %1$s() RETURNS trigger AS $$
            BEGIN
              IF TG_OP = 'DELETE' THEN
                %2$s
                RETURN OLD;
              ELSIF TG_OP IN ('INSERT', 'UPDATE') THEN
                %3$s
                RETURN NEW;
              END IF;
              RETURN NULL;
            END; $$ LANGUAGE plpgsql SECURITY DEFINER;
            """
        .formatted(fnName(schema), deleteInsert, upsertInsert);
  }

  @Override
  public String buildTrigger(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    return """
            CREATE TRIGGER %1$s
              AFTER INSERT OR UPDATE OR DELETE ON %2$s
              FOR EACH ROW EXECUTE FUNCTION %3$s();
            """
        .formatted(triggerName(schema), sourceTable(schema), fnName(schema));
  }
}
