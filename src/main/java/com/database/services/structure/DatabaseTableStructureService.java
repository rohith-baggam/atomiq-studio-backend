package com.database.services.structure;

import com.database.dto.response.structure.DatabaseTableListResponse;
import com.database.dto.response.structure.DatabaseTablePropertiesColumnResource;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import com.database.utils.structure.DatabaseStructureUtility;
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
}
