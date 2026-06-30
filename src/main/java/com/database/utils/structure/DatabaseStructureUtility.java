package com.database.utils.structure;

import com.database.dto.response.structure.DatabaseTableListResponse;
import com.database.utils.database.DatabaseConnectionUtility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DatabaseStructureUtility {

  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public List<DatabaseTableListResponse> getDatabaseTableList(Connection connection)
      throws SQLException {
    List<DatabaseTableListResponse> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        String schema = rs.getString("TABLE_SCHEM");
        String tableName = rs.getString("TABLE_NAME");

        String qualified = "\"" + schema + "\".\"" + tableName + "\"";

        long rowCount = 0;
        try (Statement st = connection.createStatement();
            ResultSet countRs = st.executeQuery("SELECT COUNT(*) FROM " + qualified)) {
          countRs.next();
          rowCount = countRs.getLong(1);
        }

        results.add(new DatabaseTableListResponse(tableName, rowCount));
      }
    }
    return results;
  }

  public void getDatabaseTablePropertyDetails(Connection connection, String tableName) {}
}
