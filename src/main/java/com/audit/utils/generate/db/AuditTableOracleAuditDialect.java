package com.audit.utils.generate.db;

import com.audit.utils.generate.AuditPayloadStrategy;
import com.audit.utils.generate.AuditTableAuditSchemaConfig;
import com.audit.utils.generate.AuditTableAuditTypeRenderer;
import com.audit.utils.generate.AuditTableGenerateAuditDialect;
import com.audit.utils.generate.AuditTableGenerateColumnMeta;
import com.audit.utils.generate.AuditTableGenerateTableSchema;
import java.util.stream.Collectors;

public final class AuditTableOracleAuditDialect implements AuditTableGenerateAuditDialect {

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
    return strategy == AuditPayloadStrategy.TWIN_TABLE_EXPLICIT;
  }

  private void requireSupported(AuditPayloadStrategy strategy) {
    if (!supports(strategy)) {
      throw new UnsupportedOperationException(
          "Audit payload strategy " + strategy + " is not supported for Oracle");
    }
  }

  private String auditSchema() {
    return quoteIdent(AuditTableAuditSchemaConfig.AUDIT_SCHEMA);
  }

  @Override
  public String createAuditSchemaStatement() {
    // In Oracle a schema is a user account (needs a password/tablespace/quota), so it
    // can't be safely auto-provisioned here; the DBA must create/grant it up front.
    return "-- Oracle: ensure user/schema "
        + auditSchema()
        + " exists and this connection has privileges on it before running the rest of this script.";
  }

  private String auditTable(AuditTableGenerateTableSchema schema) {
    return auditSchema() + "." + quoteIdent(schema.table() + "_audit");
  }

  private String sourceTable(AuditTableGenerateTableSchema schema) {
    return schema.schema() != null
        ? quoteIdent(schema.schema()) + "." + quoteIdent(schema.table())
        : quoteIdent(schema.table());
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
              audit_id        NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
              audit_operation CHAR(1) NOT NULL,
              audit_action_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
              audit_db_user   VARCHAR2(255) DEFAULT SYS_CONTEXT('USERENV','SESSION_USER') NOT NULL,
              audit_app_user  VARCHAR2(255),
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
    return "-- Oracle: trigger body is inline, see the CREATE OR REPLACE TRIGGER statement below.";
  }

  @Override
  public String buildTrigger(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    String insertCols =
        schema.columns().stream().map(c -> quoteIdent(c.name())).collect(Collectors.joining(", "));
    String newVals =
        schema.columns().stream()
            .map(c -> ":NEW." + quoteIdent(c.name()))
            .collect(Collectors.joining(", "));
    String oldVals =
        schema.columns().stream()
            .map(c -> ":OLD." + quoteIdent(c.name()))
            .collect(Collectors.joining(", "));

    return """
            CREATE OR REPLACE TRIGGER %1$s
              AFTER INSERT OR UPDATE OR DELETE ON %2$s
              FOR EACH ROW
            DECLARE
              v_op CHAR(1);
            BEGIN
              IF INSERTING THEN v_op := 'I';
              ELSIF UPDATING THEN v_op := 'U';
              ELSE v_op := 'D';
              END IF;

              IF DELETING THEN
                INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                  VALUES (v_op, SYS_CONTEXT('APP_CTX', 'APP_USER_ID'), %6$s);
              ELSE
                INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                  VALUES (v_op, SYS_CONTEXT('APP_CTX', 'APP_USER_ID'), %5$s);
              END IF;
            END;
            /
            """
        .formatted(
            quoteIdent("trg_" + schema.table() + "_audit"),
            sourceTable(schema),
            auditTable(schema),
            insertCols,
            newVals,
            oldVals);
  }
}
