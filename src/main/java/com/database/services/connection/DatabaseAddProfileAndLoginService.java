package com.database.services.connection;

import com.database.dto.request.connection.DatabaseAddProfileRequest;
import com.database.dto.request.connection.DatabaseProfileLoginRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.model.DatabaseEntity;
import com.database.model.DbUserEntity;
import com.database.repository.DatabaseEntityRepository;
import com.database.repository.DbUserEntityRepository;
import com.database.utils.database.DatabaseProfileLoginBaseUtility;
import com.shared.enums.DataBaseType;
import com.shared.enums.DatabaseConnectionStatus;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class DatabaseAddProfileAndLoginService extends DatabaseProfileLoginBaseUtility {

  @Inject public DatabaseEntityRepository databaseEntityRepository;

  @Inject public DbUserEntityRepository dbUserEntityRepository;

  @Transactional
  public DatabaseEntity getOrCreateDatabaseEntity(String dbName, DataBaseType dbType) {

    DatabaseEntity databaseEntity = databaseEntityRepository.findByNameAndType(dbName, dbType);

    if (databaseEntity != null) {
      return databaseEntity;
    }

    DatabaseEntity databaseEntitynew = new DatabaseEntity();
    databaseEntitynew.dbName = dbName;
    databaseEntitynew.dbType = dbType;
    databaseEntityRepository.persist(databaseEntitynew);
    return databaseEntitynew;
  }

  @Transactional
  public DbUserEntity createDbUserEntity(DatabaseAddProfileRequest request) {

    dbUserEntityRepository
        .findByProfileName(request.profileName)
        .ifPresent(
            existing -> {
              throw new ValidationException("Profile Name already exist");
            });

    DatabaseEntity databaseEntity = this.getOrCreateDatabaseEntity(request.dbName, request.dbType);

    dbUserEntityRepository
        .findByDbEntityNameTypeAndUsername(databaseEntity, request.username)
        .ifPresent(
            existing -> {
              throw new ValidationException("Database User already exist");
            });

    DbUserEntity dbUserEntity = new DbUserEntity();
    dbUserEntity.databaseEntity = databaseEntity;
    dbUserEntity.profileName = request.profileName;
    dbUserEntity.username = request.username;
    dbUserEntity.host = request.host;
    dbUserEntity.port = request.port;
    dbUserEntity.password = request.password;
    dbUserEntity.environment = request.environment;
    // lastConnectedTime / lastConnectionStatus are set only after the connection is
    // validated (see addDatabaseProfileAndLogin), not blindly at creation time.
    return dbUserEntity;
  }

  @Inject DatabaseProfileLoginService databaseProfileLoginService;

  @Transactional
  public DatabaseLoginResponse addDatabaseProfileAndLogin(DatabaseAddProfileRequest request) {

    DbUserEntity dbUserEntity = this.createDbUserEntity(request);

    dbUserEntityRepository.persist(dbUserEntity);

    // Validate the connection — throws ValidationException (rolling back this
    // transaction, so the profile is not saved) if the DB is unreachable.
    DatabaseLoginResponse databaseLoginResponse =
        databaseProfileLoginService.login(new DatabaseProfileLoginRequest(dbUserEntity.id));

    // On success only: record the successful connection. dbUserEntity is a managed
    // entity, so these updates are flushed when the transaction commits.
    dbUserEntity.lastConnectedTime = LocalDateTime.now();
    dbUserEntity.lastConnectionStatus = DatabaseConnectionStatus.CONNECTED;

    return databaseLoginResponse;
  }
}
