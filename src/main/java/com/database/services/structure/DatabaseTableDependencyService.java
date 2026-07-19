package com.database.services.structure;

import com.database.dto.request.generic.DatabaseDbTableNameGenericRequest;
import com.database.dto.response.er_diagram.DatabaseErDiagramResponse;
import com.database.dto.response.structure.DatabaseTableDependentTableResponse;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import com.database.utils.structure.DatabaseTableDependencyUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;

@ApplicationScoped
public class DatabaseTableDependencyService {

  @Inject DatabaseTableDependencyUtility databaseTableDependencyUtility;

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseTableDependentTableResponse getDependencyTables(
      DatabaseDbTableNameGenericRequest request, DbUserEntity dbUserEntity) throws SQLException {
    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {
      return databaseTableDependencyUtility.getDependencyTablesOnTable(
          connection, request.tableName);
    }
  }

  public DatabaseErDiagramResponse getDbErDiagram(DbUserEntity dbUserEntity) throws SQLException {
    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {
      return databaseTableDependencyUtility.getDbErDiagram(connection);
    }
  }

  public DatabaseErDiagramResponse getTableErDiagram(
      DatabaseDbTableNameGenericRequest request, DbUserEntity dbUserEntity) throws SQLException {
    try (Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity)) {
      return databaseTableDependencyUtility.getErDiagramOnTable(connection, request.tableName);
    }
  }
}
