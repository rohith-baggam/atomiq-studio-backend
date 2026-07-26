package com.database.utils.base.transfer;

import com.database.utils.base.DatabaseTransferConnectorBase;
import java.sql.Connection;

public class DatabaseOracleTransfer extends DatabaseTransferConnectorBase {

  public DatabaseOracleTransfer(Connection connection) {
    super(connection);
  }

  @Override
  public String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String dialectName() {
    return "Oracle";
  }

  @Override
  protected String formatBoolean(boolean value) {
    // Oracle has no native BOOLEAN in SQL; represent as NUMBER(1).
    return value ? "1" : "0";
  }
}
