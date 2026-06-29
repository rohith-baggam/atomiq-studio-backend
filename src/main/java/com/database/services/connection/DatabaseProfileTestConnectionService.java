package com.database.services.connection;

import com.database.dto.request.connection.DatabaseProfileTestConnectionRequest;
import com.database.dto.response.connection.DatabaseTestConnectionResponse;
import com.database.model.DbUserEntity;
import com.database.utils.database.DatabaseConnectionUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatabaseProfileTestConnectionService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseTestConnectionResponse connect(DatabaseProfileTestConnectionRequest request) {

    DbUserEntity dbUserEntity = databaseConnectionUtility.getDbUserEntity(request.userId);
    Boolean isConnected = databaseConnectionUtility.isDatabaseEntityConnected(dbUserEntity);
    return new DatabaseTestConnectionResponse(isConnected);
  }
}
