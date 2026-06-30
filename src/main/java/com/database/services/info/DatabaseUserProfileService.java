package com.database.services.info;

import com.database.dto.request.connection.DatabaseProfileTestConnectionRequest;
import com.database.dto.response.info.DatabaseUserProfileResponse;
import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import com.shared.exceptions.ValidationException;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class DatabaseUserProfileService {

  @Inject public DbUserEntityRepository dbUserEntityRepository;

  public List<DatabaseUserProfileResponse> getDbUserEntityList() {
    return dbUserEntityRepository.listAll(Sort.ascending("lastConnectedTime")).stream()
        .map(
            userEntitie ->
                new DatabaseUserProfileResponse(
                    userEntitie.databaseEntity.id,
                    userEntitie.databaseEntity.dbName,
                    userEntitie.databaseEntity.dbType,
                    userEntitie.id,
                    userEntitie.profileName,
                    userEntitie.username,
                    userEntitie.host,
                    userEntitie.port,
                    userEntitie.password,
                    userEntitie.environment,
                    userEntitie.lastConnectedTime,
                    userEntitie.lastConnectionStatus))
        .toList();
  }

  @Transactional
  public boolean deleteDbUser(DatabaseProfileTestConnectionRequest request) {
    DbUserEntity dbUserEntity =
        dbUserEntityRepository
            .findByDbUserId(request.userId)
            .orElseThrow(() -> new ValidationException("Invalid Profile"));
    dbUserEntity.delete();
    return true;
  }
}
