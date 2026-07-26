package com.database.utils.base.transfer;

import com.database.utils.base.DatabaseTransferConnectorBase;
import java.sql.Connection;

public class DatabaseMssqlTransfer extends DatabaseTransferConnectorBase {

  public DatabaseMssqlTransfer(Connection connection) {
    super(connection);
  }

  @Override
  public String quoteIdentifier(String identifier) {
    return "[" + identifier.replace("]", "]]") + "]";
  }

  @Override
  public String dialectName() {
    return "Microsoft SQL Server";
  }

  @Override
  protected String formatBoolean(boolean value) {
    return value ? "1" : "0";
  }
}
