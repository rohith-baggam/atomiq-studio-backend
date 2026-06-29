package com.database.utils.base.login;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.utils.JwtUtils;

public class DatabasePostgresLogin extends DatabaseProfileLoginBase {

  DatabaseConnectionUtility databaseConnectionUtility;

  public DatabasePostgresLogin(DatabaseTestConnectionRequest databaseLoginRequest) {
    super(databaseLoginRequest);
  }

  @Override
  public String generateJwtToken(DbUserEntity dbUserEntity) {
    return JwtUtils.generateJwt(dbUserEntity);
  }

  @Override
  public String buildJdbcUrl() {
    return "jdbc:postgresql://" + request.host + ":" + request.port + "/" + request.dbName;
  }
}
