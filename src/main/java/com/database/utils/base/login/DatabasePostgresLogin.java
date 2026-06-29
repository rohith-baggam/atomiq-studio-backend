package com.database.utils.base.login;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.shared.utils.JwtUtils;

public class DatabasePostgresLogin extends DatabaseProfileLoginBase {

  public DatabasePostgresLogin(DatabaseTestConnectionRequest databaseLoginRequest) {
    super(databaseLoginRequest);
  }

  @Override
  public String generateJwtToken(DbUserEntity dbUserEntity) {
    return JwtUtils.generateJwt(dbUserEntity);
  }

  @Override
  public boolean testConnectionRequest() {
    return false;
  }
}
