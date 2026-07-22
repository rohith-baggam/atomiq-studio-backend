package com.database.services.settings;

import com.database.dto.request.settings.DatabaseUpdatePasswordRequest;
import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class DatabaseSettingsService {

  @Inject public DbUserEntityRepository dbUserEntityRepository;

  /** Updates the stored password of a saved profile. */
  @Transactional
  public boolean updatePassword(DatabaseUpdatePasswordRequest request) {
    DbUserEntity dbUserEntity =
        dbUserEntityRepository
            .findByDbUserId(request.userId)
            .orElseThrow(() -> new ValidationException("Invalid Profile"));

    dbUserEntity.password = request.password;
    // Managed entity — flushed on transaction commit.
    return true;
  }
}
