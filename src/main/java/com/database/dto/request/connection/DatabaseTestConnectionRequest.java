package com.database.dto.request.connection;

import com.shared.enums.DataBaseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// This is to test connection before adding database
public class DatabaseTestConnectionRequest {
  @NotBlank(message = "Database name is required field")
  @Size(max = 128)
  public String dbName;

  @NotNull(message = "db type is required field")
  public DataBaseType dbType;

  @NotBlank(message = "username is required field")
  @Size(max = 128)
  public String username;

  @NotBlank(message = "host is required field")
  @Size(max = 128)
  public String host;

  @NotNull(message = "Port is required field")
  @Min(value = 0)
  public Integer port;

  @NotBlank(message = "password is required field")
  @Size(max = 128)
  public String password;

  public DatabaseTestConnectionRequest(
      String dbName,
      DataBaseType dbType,
      String username,
      String host,
      Integer port,
      String password) {
    this.dbName = dbName;
    this.dbType = dbType;
    this.username = username;
    this.host = host;
    this.port = port;
    this.password = password;
  }
}
