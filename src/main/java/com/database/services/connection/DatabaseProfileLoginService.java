package com.database.services.connection;

import com.database.dto.request.connection.DatabaseProfileLoginRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.repository.DbUserEntityRepository;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.enums.DatabaseConnectionStatus;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

@ApplicationScoped
public class DatabaseProfileLoginService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  @Inject DbUserEntityRepository dbUserEntityRepository;

  @Transactional
  public DatabaseLoginResponse login(DatabaseProfileLoginRequest request) {
    String jwtToken = databaseConnectionUtility.getJwtToken(request.userId);

    boolean isConnected = databaseConnectionUtility.isConnected(request.userId);

    if (!isConnected) {
      throw new ValidationException("Database connection failed");
    }

    // Record the successful connection so the profile list can sort by most
    // recently connected. Applies to every login path — re-login to a saved
    // profile as well as add-profile-then-login. The entity is managed, so
    // these updates flush when the transaction commits.
    dbUserEntityRepository
        .findByDbUserId(request.userId)
        .ifPresent(
            dbUserEntity -> {
              dbUserEntity.lastConnectedTime = LocalDateTime.now();
              dbUserEntity.lastConnectionStatus = DatabaseConnectionStatus.CONNECTED;
            });

    return new DatabaseLoginResponse(jwtToken, isConnected);
  }
}
