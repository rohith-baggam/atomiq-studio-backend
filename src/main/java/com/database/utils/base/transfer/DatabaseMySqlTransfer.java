package com.database.utils.base.transfer;

import com.database.utils.base.DatabaseTransferConnectorBase;
import java.sql.Connection;

public class DatabaseMySqlTransfer extends DatabaseTransferConnectorBase {

  public DatabaseMySqlTransfer(Connection connection) {
    super(connection);
  }

  @Override
  public String quoteIdentifier(String identifier) {
    return "`" + identifier.replace("`", "``") + "`";
  }

  @Override
  public String dialectName() {
    return "MySQL";
  }

  @Override
  protected String formatBoolean(boolean value) {
    return value ? "1" : "0";
  }

  /**
   * MySQL's Connector/J only streams a ResultSet row-by-row when the fetch size is exactly {@code
   * Integer.MIN_VALUE} (any positive value still buffers the whole table in the client). This is
   * the documented sentinel for row-streaming and is what keeps a multi-GB export from OOM'ing
   * here.
   */
  @Override
  protected int streamingFetchSize() {
    return Integer.MIN_VALUE;
  }
}
