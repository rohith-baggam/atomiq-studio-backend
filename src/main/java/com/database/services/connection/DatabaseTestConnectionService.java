package com.database.services.connection;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.dto.response.connection.DatabaseTestConnectionResponse;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.database.utils.base.login.DatabaseMySqlLogin;
import com.database.utils.base.login.DatabasePostgresLogin;
import com.shared.enums.DataBaseType;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatabaseTestConnectionService {

  public DatabaseProfileLoginBase getDatabaseUtility(DatabaseTestConnectionRequest request) {
    if (request.dbType.toString().equals(DataBaseType.POSTGRES.toString())) {
      return new DatabasePostgresLogin(request);
    }
    if (request.dbType.toString().equals(DataBaseType.MYSQL.toString())) {
      return new DatabaseMySqlLogin(request);
    }
    throw new ValidationException("Postgres and MySQL are only available not this moment");
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
