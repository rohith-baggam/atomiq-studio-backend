package com.database.utils.base.login;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.utils.JwtUtils;

public class DatabaseOracleLogin extends DatabaseProfileLoginBase {

  DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseOracleLogin(DatabaseTestConnectionRequest databaseLoginRequest) {
    super(databaseLoginRequest);
  }

  @Override
  public String generateJwtToken(DbUserEntity dbUserEntity) {
    return JwtUtils.generateJwt(dbUserEntity);
  }

  @Override
  public String buildJdbcUrl() {
    // Oracle uses a service name (e.g. FREEPDB1) rather than a plain database name.
    return "jdbc:oracle:thin:@//" + request.host + ":" + request.port + "/" + request.dbName;
  }
}
