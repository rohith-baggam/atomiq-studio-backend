package com.database.dto.response.connection;

public class DatabaseTestConnectionResponse {

  public Boolean isConnected;

  public DatabaseTestConnectionResponse(Boolean isConnected) {
    this.isConnected = isConnected;
  }
}
