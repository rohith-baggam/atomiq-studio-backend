package com.database.utils.transfer;

import com.database.dto.request.transfer.DatabaseExportRequest;
import com.database.dto.request.transfer.DatabaseImportRequest;
import com.database.dto.response.transfer.DatabaseExportResponse;
import com.database.dto.response.transfer.DatabaseImportResponse;
import com.database.utils.base.DatabaseTransferConnectorBase;
import com.database.utils.base.transfer.DatabaseMssqlTransfer;
import com.database.utils.base.transfer.DatabaseMySqlTransfer;
import com.database.utils.base.transfer.DatabaseOracleTransfer;
import com.database.utils.base.transfer.DatabasePostgresTransfer;
import com.shared.enums.DataBaseType;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Picks the right {@link DatabaseTransferConnectorBase} for a connection's dialect and drives the
 * import / export flows on top of it.
 */
@ApplicationScoped
public class DatabaseTransferUtility {

  public DatabaseTransferConnectorBase getConnector(Connection connection, DataBaseType dbType) {
    if (dbType == null) {
      throw new ValidationException("Unknown database type");
    }
    switch (dbType) {
      case POSTGRES:
        return new DatabasePostgresTransfer(connection);
      case MYSQL:
        return new DatabaseMySqlTransfer(connection);
      case MSSQL:
        return new DatabaseMssqlTransfer(connection);
      case ORACLE:
        return new DatabaseOracleTransfer(connection);
      default:
        throw new ValidationException(
            "Only Postgres, MySQL, MSSQL and Oracle database types are supported");
    }
  }

  /**
   * Rows per table emitted in an inline (non-file) export, so a preview can't OOM on a huge table.
   */
  private static final int PREVIEW_MAX_ROWS_PER_TABLE = 200;

  public DatabaseExportResponse exportDatabase(
      Connection connection, DataBaseType dbType, String dbName, DatabaseExportRequest request)
      throws SQLException {

    DatabaseTransferConnectorBase connector = this.getConnector(connection, dbType);
    String generatedAt = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    String header =
        "-- "
            + connector.dialectName()
            + " export of \""
            + dbName
            + "\" generated at "
            + generatedAt
            + "\n\n";
    String fileName = dbName + "_export_" + OffsetDateTime.now().toEpochSecond() + ".sql";

    // --- Stream straight to disk: bounded memory, the path used for real (possibly
    // multi-GB) exports in the desktop app. ---------------------------------------
    if (request.filePath != null && !request.filePath.isBlank()) {
      Path path = Path.of(request.filePath);
      DatabaseTransferConnectorBase.ExportStats stats;
      try (BufferedWriter out = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        out.write(header);
        stats =
            connector.exportToWriter(
                out, request.includeSchema, request.includeData, request.tableNames, 0);
      } catch (IOException e) {
        throw new SQLException("Failed writing export file: " + e.getMessage(), e);
      }
      long bytes;
      try {
        bytes = Files.size(path);
      } catch (IOException e) {
        bytes = -1;
      }
      DatabaseExportResponse response = new DatabaseExportResponse();
      response.fileName = fileName;
      response.filePath = request.filePath;
      response.sql = null;
      response.byteCount = bytes;
      response.truncated = false;
      response.tableCount = stats.tableCount;
      response.includeData = request.includeData;
      response.generatedAt = generatedAt;
      return response;
    }

    // --- Inline: bounded to a per-table sample so it is safe to hold in memory and
    // ship in the JSON response (used by the browser/dev build and the preview). ---
    StringWriter sw = new StringWriter();
    DatabaseTransferConnectorBase.ExportStats stats;
    try {
      sw.write(header);
      stats =
          connector.exportToWriter(
              sw,
              request.includeSchema,
              request.includeData,
              request.tableNames,
              PREVIEW_MAX_ROWS_PER_TABLE);
    } catch (IOException e) {
      // StringWriter never throws IOException; map defensively.
      throw new SQLException(e.getMessage(), e);
    }
    DatabaseExportResponse response =
        new DatabaseExportResponse(
            fileName, sw.toString(), stats.tableCount, request.includeData, generatedAt);
    response.truncated = stats.truncated;
    return response;
  }

  public DatabaseImportResponse importDatabase(
      Connection connection, DataBaseType dbType, DatabaseImportRequest request)
      throws SQLException {

    DatabaseTransferConnectorBase connector = this.getConnector(connection, dbType);

    // Stream from disk when a path is given — never loads a multi-GB script into memory.
    if (request.filePath != null && !request.filePath.isBlank()) {
      try (BufferedReader in =
          Files.newBufferedReader(Path.of(request.filePath), StandardCharsets.UTF_8)) {
        return connector.importFromReader(in, request.stopOnError);
      } catch (IOException e) {
        throw new SQLException("Failed reading import file: " + e.getMessage(), e);
      }
    }

    if (request.script == null || request.script.isBlank()) {
      throw new ValidationException("Either script or filePath is required");
    }
    return connector.importFromSql(request.script, request.stopOnError);
  }
}
