package com.database.utils.base.login;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.utils.JwtUtils;

public class DatabaseMssqlLogin extends DatabaseProfileLoginBase {

  DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseMssqlLogin(DatabaseTestConnectionRequest databaseLoginRequest) {
    super(databaseLoginRequest);
  }

  @Override
  public String generateJwtToken(DbUserEntity dbUserEntity) {
    return JwtUtils.generateJwt(dbUserEntity);
  }

  @Override
  public String buildJdbcUrl() {
    // encrypt=false keeps local/dev connections working without a trusted TLS cert.
    return "jdbc:sqlserver://"
        + request.host
        + ":"
        + request.port
        + ";databaseName="
        + request.dbName
        + ";encrypt=false;trustServerCertificate=true";
  }
}
