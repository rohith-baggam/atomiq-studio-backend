package com.database.utils.database;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.model.DbUserEntity;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.base.login.DatabaseMssqlLogin;
import com.database.utils.base.login.DatabaseMySqlLogin;
import com.database.utils.base.login.DatabaseOracleLogin;
import com.database.utils.base.login.DatabasePostgresLogin;
import com.shared.enums.DataBaseType;
import com.shared.exceptions.ValidationException;

public class DatabaseProfileLoginBaseUtility {

  public DatabaseProfileLoginBase getDatabaseUtility(DbUserEntity dbUserEntity) {
    DatabaseTestConnectionRequest databaseTestConnectionRequest =
        this.getDatabaseTestConnectionRequest(dbUserEntity);
    return this.getDatabaseUtilityWithRequest(databaseTestConnectionRequest);
  }

  public DatabaseTestConnectionRequest getDatabaseTestConnectionRequest(DbUserEntity dbUserEntity) {

    DatabaseTestConnectionRequest databaseTestConnectionRequest =
        new DatabaseTestConnectionRequest(
            dbUserEntity.databaseEntity.dbName,
            dbUserEntity.databaseEntity.dbType,
            dbUserEntity.username,
            dbUserEntity.host,
            dbUserEntity.port,
            dbUserEntity.password);
    return databaseTestConnectionRequest;
  }

  public DatabaseProfileLoginBase getDatabaseUtilityWithRequest(
      DatabaseTestConnectionRequest databaseTestConnectionRequest) {

    if (databaseTestConnectionRequest.dbType.toString().equals(DataBaseType.POSTGRES.toString())) {
      return new DatabasePostgresLogin(databaseTestConnectionRequest);
    }
    if (databaseTestConnectionRequest.dbType.toString().equals(DataBaseType.MYSQL.toString())) {
      return new DatabaseMySqlLogin(databaseTestConnectionRequest);
    }
    if (databaseTestConnectionRequest.dbType.toString().equals(DataBaseType.MSSQL.toString())) {
      return new DatabaseMssqlLogin(databaseTestConnectionRequest);
    }
    if (databaseTestConnectionRequest.dbType.toString().equals(DataBaseType.ORACLE.toString())) {
      return new DatabaseOracleLogin(databaseTestConnectionRequest);
    }
    throw new ValidationException(
        "Only Postgres, MySQL, MSSQL and Oracle database types are supported");
  }

  public DatabaseLoginResponse login(DbUserEntity dbUserEntity) {
    DatabaseProfileLoginBase databaseProfileLoginBase = this.getDatabaseUtility(dbUserEntity);
    String jwtToken = databaseProfileLoginBase.generateJwtToken(dbUserEntity);
    DatabaseLoginResponse databaseLoginResponse = databaseProfileLoginBase.login(jwtToken);
    return databaseLoginResponse;
  }
}
