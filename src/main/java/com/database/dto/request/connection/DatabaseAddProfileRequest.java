package com.database.dto.request.connection;

import com.shared.enums.DataBaseType;
import com.shared.enums.DatabaseEnvironment;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// for database first time login to test and save database

public class DatabaseAddProfileRequest {
  @NotBlank(message = "Database name is required field")
  @Size(max = 128)
  public String dbName;

  @NotNull(message = "db type is required field")
  @Enumerated(EnumType.STRING)
  public DataBaseType dbType;

  @NotBlank(message = "profile name is required field")
  @Size(max = 128)
  public String profileName;

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

  @NotNull(message = "environment is required field")
  public DatabaseEnvironment environment;

  public DatabaseAddProfileRequest(
      String dbName,
      DataBaseType dbType,
      String profileName,
      String username,
      String host,
      Integer port,
      String password,
      DatabaseEnvironment environment) {
    this.dbName = dbName;
    this.profileName = profileName;
    this.dbType = dbType;
    this.username = username;
    this.host = host;
    this.port = port;
    this.password = password;
    this.environment = environment;
  }
}
