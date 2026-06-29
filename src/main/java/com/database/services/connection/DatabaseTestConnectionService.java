package com.database.services.connection;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.dto.response.connection.DatabaseTestConnectionResponse;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.base.login.DatabaseMySqlLogin;
import com.database.utils.base.login.DatabasePostgresLogin;
import com.shared.enums.DataBaseType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ValidationException;

@ApplicationScoped
public class DatabaseTestConnectionService {

  public DatabaseProfileLoginBase getDatabaseUtility(DatabaseTestConnectionRequest request) {
    if (request.dbType.toString().equals(DataBaseType.POSTGRES.toString())) {
      return new DatabasePostgresLogin(request);
    }
    if (request.dbType.toString().equals(DataBaseType.MYSQL.toString())) {
      return new DatabaseMySqlLogin(request);
    }
    throw new ValidationException("Postgres and MSSQL are only available not this moment");
  }

  public DatabaseTestConnectionResponse testConnect(DatabaseTestConnectionRequest request) {

    DatabaseProfileLoginBase databaseProfileLoginBase = this.getDatabaseUtility(request);
    boolean isConnected = databaseProfileLoginBase.testConnectionRequest();
    if (!isConnected) {
      throw new ValidationException("Database connection failed");
    }
    return new DatabaseTestConnectionResponse(isConnected);
  }
}
