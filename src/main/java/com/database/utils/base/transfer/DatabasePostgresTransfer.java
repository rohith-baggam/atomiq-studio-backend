package com.database.utils.base.transfer;

import com.database.utils.base.DatabaseTransferConnectorBase;
import java.sql.Connection;

public class DatabasePostgresTransfer extends DatabaseTransferConnectorBase {

  public DatabasePostgresTransfer(Connection connection) {
    super(connection);
  }

  @Override
  public String quoteIdentifier(String identifier) {
    return "\"" + identifier.replace("\"", "\"\"") + "\"";
  }

  @Override
  public String dialectName() {
    return "PostgreSQL";
  }
}
