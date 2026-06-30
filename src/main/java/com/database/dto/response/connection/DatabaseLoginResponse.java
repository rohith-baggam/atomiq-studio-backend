package com.database.dto.response.connection;

public class DatabaseLoginResponse {
  public String jwtToken;
  public Boolean isConnected;

  public DatabaseLoginResponse(String jwtToken, Boolean isConnected) {
    this.jwtToken = jwtToken;
    this.isConnected = isConnected;
  }
}
