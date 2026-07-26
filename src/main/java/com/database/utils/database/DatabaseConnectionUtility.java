package com.database.utils.database;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import com.database.services.connection.DatabaseTestConnectionService;
import com.database.utils.base.DatabaseProfileLoginBase;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class DatabaseConnectionUtility extends DatabaseProfileLoginBaseUtility {

  @Inject DbUserEntityRepository databaseUserEntityRepository;

  @Inject DatabaseTestConnectionService databaseTestConnectionService;

  // Bounds only the login handshake, so a dead/unreachable host fails fast
  // instead of blocking a worker thread. Query duration is unaffected — that's
  // governed by the per-statement query timeout, not this.
  @ConfigProperty(name = "atomiq.db.connect-timeout-seconds", defaultValue = "15")
  int connectTimeoutSeconds;

  public DbUserEntity getDbUserEntity(UUID dbUserEntityId) {
    DbUserEntity dbUserEntity =
        databaseUserEntityRepository
            .findByDbUserId(dbUserEntityId)
            .orElseThrow(() -> new ValidationException("Invalid Profile"));
    return dbUserEntity;
  }

  public String getJwtToken(UUID dbUserEntityId) {

    DbUserEntity dbUserEntity = this.getDbUserEntity(dbUserEntityId);

    return this.login(dbUserEntity).jwtToken;
  }

  // get database connection health with dbUserEntity id
  public boolean isConnected(UUID dbUserEntityId) {

    DbUserEntity dbUserEntity = this.getDbUserEntity(dbUserEntityId);
    DatabaseTestConnectionRequest databaseTestConnectionRequest =
        new DatabaseTestConnectionRequest(
            dbUserEntity.databaseEntity.dbName,
            dbUserEntity.databaseEntity.dbType,
            dbUserEntity.username,
            dbUserEntity.host,
            dbUserEntity.port,
            dbUserEntity.password);

    return this.isDatabaseRequestConnected(databaseTestConnectionRequest);
  }

  public String getDatabaseConnectionString(DbUserEntity dbUserEntity) {

    DatabaseProfileLoginBase databaseTestConnectionBase = this.getDatabaseUtility(dbUserEntity);
    return databaseTestConnectionBase.buildJdbcUrl();
  }

  public String getDatabaseConnectionStringWithRequest(DatabaseTestConnectionRequest request) {

    String jdbcUrl = this.getDatabaseUtilityWithRequest(request).buildJdbcUrl();
    return jdbcUrl;
  }

  public Connection getDatabaseConnectionWithTestConnectionRequest(
      DatabaseTestConnectionRequest databaseTestConnectionRequest) throws SQLException {

    String jdbcUrl = this.getDatabaseConnectionStringWithRequest(databaseTestConnectionRequest);
    // Global login timeout applied to every JDBC driver (covers Oracle/MSSQL,
    // whose URL param names differ). Only the handshake is bounded.
    DriverManager.setLoginTimeout(connectTimeoutSeconds);
    Connection connection =
        DriverManager.getConnection(
            jdbcUrl,
            databaseTestConnectionRequest.username,
            databaseTestConnectionRequest.password);
    return connection;
  }

  public Connection getDatabaseConnection(DbUserEntity dbUserEntity) throws SQLException {

    DatabaseTestConnectionRequest request = this.getDatabaseTestConnectionRequest(dbUserEntity);
    return this.getDatabaseConnectionWithTestConnectionRequest(request);
  }

  public boolean isDatabaseEntityConnected(DbUserEntity dbUserEntity) {
    try (Connection connection = this.getDatabaseConnection(dbUserEntity)) {
      return connection.isValid(10);

    } catch (SQLException e) {
      return false;
    }
  }

  public boolean isDatabaseRequestConnected(
      DatabaseTestConnectionRequest databaseTestConnectionRequest) {
    try (Connection connection =
        this.getDatabaseConnectionWithTestConnectionRequest(databaseTestConnectionRequest)) {

      return connection.isValid(10);

    } catch (SQLException e) {
      return false;
    }
  }
}
