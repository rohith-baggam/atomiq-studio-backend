package com.database.utils.base.login;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.utils.JwtUtils;

public class DatabaseMySqlLogin extends DatabaseProfileLoginBase {

  DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseMySqlLogin(DatabaseTestConnectionRequest databaseLoginRequest) {
    super(databaseLoginRequest);
  }

  @Override
  public String generateJwtToken(DbUserEntity dbUserEntity) {

    return JwtUtils.generateJwt(dbUserEntity);
  }

  @Override
  public String buildJdbcUrl() {
    // socketTimeout=0 → never abort a long-running read (big joins / schema
    // pulls). The connect handshake is bounded separately via DriverManager's
    // login timeout, and per-statement limits via the query timeout.
    return "jdbc:mysql://"
        + request.host
        + ":"
        + request.port
        + "/"
        + request.dbName
        + "?socketTimeout=0";
  }
}
