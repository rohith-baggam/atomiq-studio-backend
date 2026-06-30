package com.database.utils.structure;

import com.database.dto.response.structure.DatabaseTableDependentTableResponse;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

@ApplicationScoped
public class DatabaseTableDependencyUtility {

  @Inject DatabaseStructureUtility databaseStructureUtility;
  @Inject DatabaseConnectionUtility databaseConnectionUtility;

  public DatabaseTableDependentTableResponse getDependencyTablesOnTable(
      Connection connection, String tableName) throws SQLException {

    if (!databaseStructureUtility.isTableNameExist(connection, tableName)) {
      throw new ValidationException("Invalid tableName");
    }

    DatabaseMetaData meta = connection.getMetaData();

    Set<String> dependentOn = new LinkedHashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next()) {
        dependentOn.add(rs.getString("PKTABLE_NAME"));
      }
    }

    Set<String> dependentBy = new LinkedHashSet<>();
    try (ResultSet rs = meta.getExportedKeys(null, null, tableName)) {
      while (rs.next()) {
        dependentBy.add(rs.getString("FKTABLE_NAME"));
      }
    }

    return new DatabaseTableDependentTableResponse(
        new ArrayList<>(dependentOn), new ArrayList<>(dependentBy));
  }
}
