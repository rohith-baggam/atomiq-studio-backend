package com.database.utils.structure;

import com.database.dto.response.structure.DatabaseTableListResponse;
import com.database.dto.response.structure.DatabaseTablePropertiesColumnResource;
import com.database.utils.database.DatabaseConnectionUtility;
import com.shared.exceptions.ValidationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        long columnCount = 0;
        try (ResultSet colRs = meta.getColumns(null, schema, tableName, "%")) {
          while (colRs.next()) columnCount++;
        }
        // we need to add no of columns
        results.add(new DatabaseTableListResponse(tableName, rowCount, columnCount));
      }
    }
    return results;
  }

  public boolean isTableNameExist(Connection connection, String tableName) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();
    try (ResultSet rs = meta.getTables(null, null, tableName, new String[] {"TABLE"})) {
      return rs.next();
    }
  }

  public List<DatabaseTablePropertiesColumnResource> getDatabaseTableColumnPropertyDetails(
      Connection connection, String tableName) throws SQLException {

    if (!this.isTableNameExist(connection, tableName)) {
      throw new ValidationException("Invalid tableName");
    }
    List<DatabaseTablePropertiesColumnResource> results = new ArrayList<>();
    DatabaseMetaData meta = connection.getMetaData();

    // 1. primary-key columns
    Set<String> pkCols = new HashSet<>();
    try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
      while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
    }

    // 2. foreign-key columns
    Set<String> fkCols = new HashSet<>();
    try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
      while (rs.next()) fkCols.add(rs.getString("FKCOLUMN_NAME"));
    }

    // 3. indexed + unique columns
    Set<String> indexedCols = new HashSet<>();
    Set<String> uniqueCols = new HashSet<>();
    try (ResultSet rs = meta.getIndexInfo(null, null, tableName, false, false)) {
      while (rs.next()) {
        String col = rs.getString("COLUMN_NAME");
        if (col == null) continue; // stats row → skip
        indexedCols.add(col);
        if (!rs.getBoolean("NON_UNIQUE")) uniqueCols.add(col);
      }
    }

    // 4. columns — merge the sets in
    try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
      while (rs.next()) {
        String columnName = rs.getString("COLUMN_NAME");
        results.add(
            new DatabaseTablePropertiesColumnResource(
                columnName,
                rs.getString("TYPE_NAME"),
                rs.getInt("COLUMN_SIZE"),
                rs.getInt("DECIMAL_DIGITS"),
                rs.getInt("ORDINAL_POSITION"),
                rs.getInt("NULLABLE") == DatabaseMetaData.columnNoNulls,
                rs.getString("COLUMN_DEF"),
                rs.getString("IS_AUTOINCREMENT"),
                rs.getString("REMARKS"),
                pkCols.contains(columnName),
                fkCols.contains(columnName),
                indexedCols.contains(columnName),
                uniqueCols.contains(columnName)));
      }
    }

    return results;
  }
}
