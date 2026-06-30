package com.database.services.structure;

import com.database.dto.request.generic.DatabaseDbTableNameGenericRequest;
import com.database.dto.request.info.DatabaseDbTableDataRequest;
import com.database.dto.request.info.table_properties.DatabaseTableDetailResponse;
import com.database.dto.response.info.DatabaseTableFieldsHelperListResponse;
import com.database.dto.response.structure.DatabaseTableDataResponse;
import com.database.dto.response.structure.DatabaseTableListResponse;
import com.database.dto.response.structure.DatabaseTablePropertiesColumnResource;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import com.database.utils.structure.DatabaseStructureUtility;
import com.database.utils.structure.DatabaseTableDependencyUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@ApplicationScoped
public class DatabaseTableStructureService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  @Inject DatabaseStructureUtility databaseStructureUtility;

  public List<DatabaseTableListResponse> getDatabaseTableList(DbUserEntity dbUserEntity)
      throws SQLException {
    Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity);
    return databaseStructureUtility.getDatabaseTableList(connection);
  }

  public List<DatabaseTablePropertiesColumnResource> getDatabaseFieldProperties(
      DbUserEntity dbUserEntity, String tableName) throws SQLException {
    Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity);
    return databaseStructureUtility.getDatabaseTableColumnPropertyDetails(connection, tableName);
  }

  public DatabaseTableDataResponse getDbTabledata(
      DbUserEntity dbUserEntity, DatabaseDbTableDataRequest request) throws SQLException {

    Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity);
    return databaseStructureUtility.getDbTabledata(connection, request);
  }

  @Inject DatabaseTableDependencyUtility databaseTableDependencyUtility;

  public DatabaseTableDetailResponse getDbTableDetails(
      DbUserEntity dbUserEntity, DatabaseDbTableNameGenericRequest request) throws SQLException {
    Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity);
    return databaseStructureUtility.getDbTableDetails(
        connection,
        request.tableName,
        databaseTableDependencyUtility.getDependencyTablesOnTable(connection, request.tableName));
  }

  public List<DatabaseTableFieldsHelperListResponse> tableFieldHelperList(
      DbUserEntity dbUserEntity, DatabaseDbTableNameGenericRequest request) throws SQLException {
    Connection connection = databaseConnectionUtility.getDatabaseConnection(dbUserEntity);
    return databaseStructureUtility.tableFieldHelperList(connection, request.tableName);
  }
}
