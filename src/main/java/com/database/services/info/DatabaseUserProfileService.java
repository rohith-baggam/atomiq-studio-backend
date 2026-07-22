package com.database.services.info;

import com.database.dto.request.connection.DatabaseProfileTestConnectionRequest;
import com.database.dto.request.connection.DatabaseUpdateProfileRequest;
import com.database.dto.response.info.DatabaseUserProfileResponse;
import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import com.shared.exceptions.ValidationException;
import com.shared.security.CurrentUser;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class DatabaseUserProfileService {

  @Inject public DbUserEntityRepository dbUserEntityRepository;

  @Inject CurrentUser currentUser;

  public List<DatabaseUserProfileResponse> getDbUserEntityList() {
    return dbUserEntityRepository.listAll(Sort.descending("lastConnectedTime")).stream()
        .map(this::toResponse)
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

  public DatabaseUserProfileResponse entityDetails() {
    return toResponse(currentUser.getUser());
  }

  /** Partial update: only non-null request fields are applied. */
  @Transactional
  public DatabaseUserProfileResponse updateProfile(DatabaseUpdateProfileRequest request) {
    DbUserEntity dbUserEntity =
        dbUserEntityRepository
            .findByDbUserId(request.userId)
            .orElseThrow(() -> new ValidationException("Invalid Profile"));

    if (request.environment != null) {
      dbUserEntity.environment = request.environment;
    }
    if (request.readOnly != null) {
      dbUserEntity.readOnly = request.readOnly;
    }
    if (request.profileName != null && !request.profileName.isBlank()) {
      dbUserEntity.profileName = request.profileName;
    }
    // Managed entity — flushed on transaction commit.
    return toResponse(dbUserEntity);
  }

  private DatabaseUserProfileResponse toResponse(DbUserEntity u) {
    return new DatabaseUserProfileResponse(
        u.databaseEntity.id,
        u.databaseEntity.dbName,
        u.databaseEntity.dbType,
        u.id,
        u.profileName,
        u.username,
        u.host,
        u.port,
        u.password,
        u.environment,
        u.lastConnectedTime,
        u.lastConnectionStatus,
        u.readOnly);
  }
}
