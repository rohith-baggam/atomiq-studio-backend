package com.database.utils.database;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import com.database.services.connection.DatabaseTestConnectionService;
import com.database.utils.base.DatabaseProfileLoginBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import java.util.UUID;

@ApplicationScoped
public class DatabaseConnectionUtility extends DatabaseProfileLoginBaseUtility {

  @Inject DbUserEntityRepository databaseUserEntityRepository;

  @Inject DatabaseTestConnectionService databaseTestConnectionService;

  public DbUserEntity getDbUserEntity(UUID dbUserEntityId) {
    DbUserEntity dbUserEntity =
        databaseUserEntityRepository
            .findByDbUserId(dbUserEntityId)
            .orElseThrow(() -> new ValidationException("Invalid Profile"));
    return dbUserEntity;
  }

  public String getJwtToken(UUID dbUserEntityId) {

    DbUserEntity dbUserEntity = this.getDbUserEntity(dbUserEntityId);

    return this.login(dbUserEntity).jwtToken;
  }

  // get database connection health with dbUserEntity id
  public boolean isConnected(UUID dbUserEntityId) {

    DbUserEntity dbUserEntity = this.getDbUserEntity(dbUserEntityId);
    DatabaseTestConnectionRequest databaseTestConnectionRequest =
        new DatabaseTestConnectionRequest(
            dbUserEntity.databaseEntity.dbName,
            dbUserEntity.databaseEntity.dbType,
            dbUserEntity.username,
            dbUserEntity.host,
            dbUserEntity.port,
            dbUserEntity.password);

    DatabaseProfileLoginBase databaseTestConnectionBase =
        databaseTestConnectionService.getDatabaseUtility(databaseTestConnectionRequest);
    return databaseTestConnectionBase.testConnectionRequest();
  }

  // next step to create a function to get jdbc driver based on database type
}
