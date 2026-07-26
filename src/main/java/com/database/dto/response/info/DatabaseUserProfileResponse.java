package com.database.dto.response.info;

import com.shared.enums.DataBaseType;
import com.shared.enums.DatabaseConnectionStatus;
import com.shared.enums.DatabaseEnvironment;
import java.time.LocalDateTime;
import java.util.UUID;

public class DatabaseUserProfileResponse {
  public UUID dbId;
  public String dbName;
  public DataBaseType dbType;
  public UUID userId;
  public String profileName;
  public String username;
  public String host;
  public Integer port;
  public String password;
  public DatabaseEnvironment environment;
  public Boolean readOnly;
  public LocalDateTime lastConnectedTime;
  public DatabaseConnectionStatus lastConnectionStatus;

  public DatabaseUserProfileResponse(
      UUID dbId,
      String dbName,
      DataBaseType dbType,
      UUID userId,
      String profileName,
      String username,
      String host,
      Integer port,
      String password,
      DatabaseEnvironment environment,
      LocalDateTime lastConnectedTime,
      DatabaseConnectionStatus lastConnectionStatus,
      Boolean readOnly) {
    this.dbId = dbId;
    this.dbName = dbName;
    this.dbType = dbType;
    this.userId = userId;
    this.profileName = profileName;
    this.username = username;
    this.host = host;
    this.port = port;
    this.environment = environment;
    this.readOnly = readOnly;
    this.lastConnectedTime = lastConnectedTime;
    this.lastConnectionStatus = lastConnectionStatus;
  }
}
