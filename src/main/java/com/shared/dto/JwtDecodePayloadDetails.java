package com.shared.dto;

import com.shared.enums.DataBaseType;

public class JwtDecodePayloadDetails {
  public String dbName;
  public DataBaseType dbType;
  public String username;
  public String host;
  public Integer port;

  public JwtDecodePayloadDetails(
      String dbName, DataBaseType dbType, String username, String host, Integer port) {
    this.dbName = dbName;
    this.dbType = dbType;
    this.username = username;
    this.host = host;
    this.port = port;
  }
}
