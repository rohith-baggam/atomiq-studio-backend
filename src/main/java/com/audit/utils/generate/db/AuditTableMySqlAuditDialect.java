package com.audit.utils.generate.db;

import com.audit.utils.generate.AuditPayloadStrategy;
import com.audit.utils.generate.AuditTableAuditSchemaConfig;
import com.audit.utils.generate.AuditTableAuditTypeRenderer;
import com.audit.utils.generate.AuditTableGenerateAuditDialect;
import com.audit.utils.generate.AuditTableGenerateColumnMeta;
import com.audit.utils.generate.AuditTableGenerateTableSchema;
import java.util.stream.Collectors;

public final class AuditTableMySqlAuditDialect implements AuditTableGenerateAuditDialect {

  @Override
  public String quoteIdent(String identifier) {
    return "`" + identifier.replace("`", "``") + "`";
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
          "Audit payload strategy " + strategy + " is not supported for MySQL");
    }
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
              audit_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
              audit_operation CHAR(1) NOT NULL,
              audit_action_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
              audit_db_user   VARCHAR(255) NOT NULL DEFAULT (CURRENT_USER()),
              audit_app_user  VARCHAR(255),
              %2$s,
              INDEX %3$s (audit_action_at)
            );
            """
        .formatted(
            auditTable(schema),
            columnsDdl,
            quoteIdent("idx_" + schema.table() + "_audit_action_at"));
  }

  @Override
  public String buildTriggerFunction(
      AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    // MySQL has no standalone trigger function object; body lives inline per trigger.
    return "-- MySQL: trigger body is inline, see the CREATE TRIGGER statements below.";
  }

  @Override
  public String buildTrigger(AuditTableGenerateTableSchema schema, AuditPayloadStrategy strategy) {
    requireSupported(strategy);
    String insertCols =
        schema.columns().stream().map(c -> quoteIdent(c.name())).collect(Collectors.joining(", "));
    String newVals =
        schema.columns().stream()
            .map(c -> "NEW." + quoteIdent(c.name()))
            .collect(Collectors.joining(", "));
    String oldVals =
        schema.columns().stream()
            .map(c -> "OLD." + quoteIdent(c.name()))
            .collect(Collectors.joining(", "));

    return """
            DELIMITER $$
            CREATE TRIGGER %1$s
              AFTER INSERT ON %2$s
              FOR EACH ROW
            BEGIN
              INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                VALUES ('I', @app_user_id, %5$s);
            END$$

            CREATE TRIGGER %6$s
              AFTER UPDATE ON %2$s
              FOR EACH ROW
            BEGIN
              INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                VALUES ('U', @app_user_id, %5$s);
            END$$

            CREATE TRIGGER %7$s
              AFTER DELETE ON %2$s
              FOR EACH ROW
            BEGIN
              INSERT INTO %3$s (audit_operation, audit_app_user, %4$s)
                VALUES ('D', @app_user_id, %8$s);
            END$$
            DELIMITER ;
            """
        .formatted(
            quoteIdent("trg_" + schema.table() + "_audit_insert"),
            sourceTable(schema),
            auditTable(schema),
            insertCols,
            newVals,
            quoteIdent("trg_" + schema.table() + "_audit_update"),
            quoteIdent("trg_" + schema.table() + "_audit_delete"),
            oldVals);
  }
}
