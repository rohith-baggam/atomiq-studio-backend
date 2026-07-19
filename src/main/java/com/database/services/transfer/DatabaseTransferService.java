package com.database.services.transfer;

import com.database.dto.request.transfer.DatabaseExportRequest;
import com.database.dto.request.transfer.DatabaseImportRequest;
import com.database.dto.response.transfer.DatabaseExportResponse;
import com.database.dto.response.transfer.DatabaseImportResponse;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import com.database.utils.transfer.DatabaseTransferUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class DatabaseTransferService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  @Inject DatabaseTransferUtility databaseTransferUtility;

  public DatabaseExportResponse exportDatabase(
      DbUserEntity dbUserEntity, DatabaseExportRequest request) throws SQLException {

    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {
      return databaseTransferUtility.exportDatabase(
          connection,
          dbUserEntity.databaseEntity.dbType,
          dbUserEntity.databaseEntity.dbName,
          request);
    }
  }

  public DatabaseImportResponse importDatabase(
      DbUserEntity dbUserEntity, DatabaseImportRequest request) throws SQLException {

    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {
      return databaseTransferUtility.importDatabase(
          connection, dbUserEntity.databaseEntity.dbType, request);
    }
  }
}
