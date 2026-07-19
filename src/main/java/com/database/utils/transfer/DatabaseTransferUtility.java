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

  public DatabaseExportResponse exportDatabase(
      Connection connection, DataBaseType dbType, String dbName, DatabaseExportRequest request)
      throws SQLException {

    DatabaseTransferConnectorBase connector = this.getConnector(connection, dbType);

    String sql = connector.exportToSql(request.includeData, request.tableNames);
    int tableCount = connector.countTables(request.tableNames);
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

    return new DatabaseExportResponse(
        fileName, header + sql, tableCount, request.includeData, generatedAt);
  }

  public DatabaseImportResponse importDatabase(
      Connection connection, DataBaseType dbType, DatabaseImportRequest request)
      throws SQLException {

    DatabaseTransferConnectorBase connector = this.getConnector(connection, dbType);
    return connector.importFromSql(request.script, request.stopOnError);
  }
}
