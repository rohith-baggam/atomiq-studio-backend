package com.audit.utils.generate;

import com.audit.utils.generate.db.AuditTableMsSqlAuditDialect;
import com.audit.utils.generate.db.AuditTableMySqlAuditDialect;
import com.audit.utils.generate.db.AuditTableOracleAuditDialect;
import com.audit.utils.generate.db.AuditTablePostgresAuditDialect;
import com.shared.exceptions.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class AuditTableGenerateScriptUtils {

  public record GeneratedAuditScripts(String jsonbScript, String twinTableScript) {}

  public GeneratedAuditScripts generateAuditCreateSchemaApi(
      Connection connection, String tableName) {
    try {
      AuditTableGenerateTableSchema schema = readTableSchema(connection, tableName);
      AuditTableGenerateAuditDialect dialect =
          resolveDialect(connection.getMetaData().getDatabaseProductName());

      String jsonbScript =
          dialect.supports(AuditPayloadStrategy.JSONB)
              ? buildScript(dialect, schema, AuditPayloadStrategy.JSONB)
              : null;
      String twinTableScript =
          dialect.supports(AuditPayloadStrategy.TWIN_TABLE_EXPLICIT)
              ? buildScript(dialect, schema, AuditPayloadStrategy.TWIN_TABLE_EXPLICIT)
              : null;

      return new GeneratedAuditScripts(jsonbScript, twinTableScript);
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to generate audit create script for table: " + tableName, e);
    }
  }

  /**
   * Runs an already-generated audit script (twin table/function/trigger DDL) as a single
   * multi-statement call. Postgres's simple query protocol parses the whole batch server-side -
   * including semicolons inside the trigger function's {@code $$...$$} body - and runs it as one
   * implicit transaction, so a failure partway through rolls back everything already applied in
   * this call rather than leaving a half-created audit setup.
   */
  public void executeScript(Connection connection, String script) throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(script);
    }
  }

  private String buildScript(
      AuditTableGenerateAuditDialect dialect,
      AuditTableGenerateTableSchema schema,
      AuditPayloadStrategy strategy) {
    StringBuilder script = new StringBuilder();
    script.append(dialect.createAuditSchemaStatement()).append("\n\n");
    script.append(dialect.buildTwinTable(schema, strategy)).append("\n\n");
    script.append(dialect.buildTriggerFunction(schema, strategy)).append("\n\n");
    script.append(dialect.buildTrigger(schema, strategy)).append("\n");
    return script.toString();
  }

  private AuditTableGenerateTableSchema readTableSchema(Connection connection, String tableName)
      throws SQLException {
    DatabaseMetaData metaData = connection.getMetaData();
    String catalog = connection.getCatalog();
    String schemaPattern = connection.getSchema();

    List<AuditTableGenerateColumnMeta> columns = new ArrayList<>();
    try (ResultSet rs = metaData.getColumns(catalog, schemaPattern, tableName, "%")) {
      while (rs.next()) {
        columns.add(
            new AuditTableGenerateColumnMeta(
                rs.getString("COLUMN_NAME"),
                rs.getString("TYPE_NAME"),
                rs.getInt("COLUMN_SIZE"),
                rs.getInt("DECIMAL_DIGITS"),
                rs.getInt("DATA_TYPE"),
                "YES".equalsIgnoreCase(rs.getString("IS_NULLABLE"))));
      }
    }

    if (columns.isEmpty()) {
      throw new ResourceNotFoundException("Table not found: " + tableName);
    }

    Set<String> primaryKeys = new LinkedHashSet<>();
    try (ResultSet rs = metaData.getPrimaryKeys(catalog, schemaPattern, tableName)) {
      while (rs.next()) {
        primaryKeys.add(rs.getString("COLUMN_NAME"));
      }
    }

    return new AuditTableGenerateTableSchema(schemaPattern, tableName, columns, primaryKeys);
  }

  private AuditTableGenerateAuditDialect resolveDialect(String productName) {
    if (productName == null) {
      throw new IllegalStateException("Unable to determine database product name");
    }
    String normalized = productName.toLowerCase();
    if (normalized.contains("postgresql")) {
      return new AuditTablePostgresAuditDialect();
    } else if (normalized.contains("mysql")) {
      return new AuditTableMySqlAuditDialect();
    } else if (normalized.contains("microsoft sql server")) {
      return new AuditTableMsSqlAuditDialect();
    } else if (normalized.contains("oracle")) {
      return new AuditTableOracleAuditDialect();
    }
    throw new UnsupportedOperationException("Unsupported database product: " + productName);
  }
}
