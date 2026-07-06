package com.audit.services;

import com.audit.dto.request.AuditExecuteSchemaRequest;
import com.audit.dto.request.AuditGenerateSchemaRequest;
import com.audit.dto.response.AuditTableCreateScriptResponse;
import com.audit.dto.response.AuditTableExecuteScriptResponse;
import com.audit.utils.generate.AuditPayloadStrategy;
import com.audit.utils.generate.AuditTableGenerateScriptUtils;
import com.audit.utils.generate.AuditTableGenerateScriptUtils.GeneratedAuditScripts;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import com.database.utils.structure.DatabaseStructureUtility;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class AuditTableConfigService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  @Inject AuditTableGenerateScriptUtils auditTableGenerateScriptUtils;

  @Inject DatabaseStructureUtility databaseStructureUtility;

  public AuditTableCreateScriptResponse generateAuditCreateSchemaApi(
      AuditGenerateSchemaRequest request, DbUserEntity dbUserEntity) throws SQLException {
    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {

      // Resolve the audit scripts first: it validates the table actually has columns and
      // throws ResourceNotFoundException otherwise, so a non-existent table name never
      // reaches getCreateTableDdl (which has no such guard and would emit an empty
      // "CREATE TABLE (...)" for it).
      GeneratedAuditScripts scripts =
          auditTableGenerateScriptUtils.generateAuditCreateSchemaApi(connection, request.tableName);

      String originalSchemaScript =
          databaseStructureUtility.getCreateTableDdl(connection, request.tableName);

      AuditPayloadStrategy strategy =
          request.auditPayloadStrategy != null
              ? request.auditPayloadStrategy
              : AuditPayloadStrategy.JSONB;

      return new AuditTableCreateScriptResponse(
          originalSchemaScript, scripts.jsonbScript(), scripts.twinTableScript(), strategy.name());
    }
  }

  public AuditTableExecuteScriptResponse executeAuditCreateSchemaApi(
      AuditExecuteSchemaRequest request, DbUserEntity dbUserEntity) throws SQLException {
    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {

      GeneratedAuditScripts scripts =
          auditTableGenerateScriptUtils.generateAuditCreateSchemaApi(connection, request.tableName);

      AuditPayloadStrategy strategy =
          request.auditPayloadStrategy != null
              ? request.auditPayloadStrategy
              : AuditPayloadStrategy.JSONB;

      String script =
          switch (strategy) {
            case JSONB -> scripts.jsonbScript();
            case TWIN_TABLE_EXPLICIT -> scripts.twinTableScript();
          };

      if (script == null) {
        throw new ValidationException(
            "Audit payload strategy " + strategy + " is not supported for this database");
      }

      auditTableGenerateScriptUtils.executeScript(connection, script);

      return new AuditTableExecuteScriptResponse(
          request.tableName, strategy.name(), script, "Audit schema created successfully");
    }
  }
}
