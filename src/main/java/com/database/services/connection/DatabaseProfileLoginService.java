package com.database.services.connection;

import com.database.dto.request.connection.DatabaseProfileLoginRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DatabaseProfileLoginService {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseLoginResponse login(DatabaseProfileLoginRequest request) {
    String jwtToken = databaseConnectionUtility.getJwtToken(request.userId);

    boolean isConnected = databaseConnectionUtility.isConnected(request.userId);

    if (!isConnected) {
      throw new ValidationException("Database connection failed");
    }
    return new DatabaseLoginResponse(jwtToken, isConnected);
  }
}
