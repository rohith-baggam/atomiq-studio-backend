package com.database.utils.base;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.model.DbUserEntity;

public abstract class DatabaseProfileLoginBase {

  public DatabaseTestConnectionRequest request;

  public DatabaseProfileLoginBase(DatabaseTestConnectionRequest request) {
    this.request = request;
  }

  public DatabaseLoginResponse login(String jwtToken) {
    DatabaseLoginResponse databaseLoginResponse = new DatabaseLoginResponse(jwtToken, true);
    return databaseLoginResponse;
  }

  public abstract String generateJwtToken(DbUserEntity dbUserEntity);

  public abstract String buildJdbcUrl();
}
