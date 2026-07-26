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
    // nullCatalogMeansCurrent=true: MySQL databases are JDBC *catalogs*, so a
    // null-catalog metadata lookup (getTables/getColumns) would otherwise scan
    // every database on the server and, when the same table name exists in more
    // than one (e.g. shopfast + shopfast_perf), return duplicate/ambiguous rows.
    // Pinning it to the connected database makes metadata scope match the other
    // engines' schema behaviour.
    return "jdbc:mysql://"
        + request.host
        + ":"
        + request.port
        + "/"
        + request.dbName
        + "?socketTimeout=0&nullCatalogMeansCurrent=true";
  }
}
