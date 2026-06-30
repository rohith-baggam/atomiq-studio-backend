package com.database.utils.structure;

import com.database.dto.response.er_diagram.DatabaseErDiagramRelationResponse;
import com.database.dto.response.er_diagram.DatabaseErDiagramResponse;
import com.database.dto.response.er_diagram.DatabaseErDiagramTableFieldQuickResponse;
import com.database.dto.response.er_diagram.DatabaseErDiagramTableResponse;
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
import java.util.List;
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

  public DatabaseErDiagramResponse getDbErDiagram(Connection connection) throws SQLException {
    DatabaseMetaData meta = connection.getMetaData();

    List<DatabaseErDiagramTableResponse> tables = new ArrayList<>();
    List<DatabaseErDiagramRelationResponse> relationships = new ArrayList<>();

    // 1. every table name
    List<String> tableNames = new ArrayList<>();
    try (ResultSet rs = meta.getTables(null, null, "%", new String[] {"TABLE"})) {
      while (rs.next()) {
        tableNames.add(rs.getString("TABLE_NAME"));
      }
    }

    for (String tableName : tableNames) {
      // PK columns
      Set<String> pkCols = new LinkedHashSet<>();
      try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
        while (rs.next()) pkCols.add(rs.getString("COLUMN_NAME"));
      }
      // FK columns
      Set<String> fkCols = new LinkedHashSet<>();
      try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
        while (rs.next()) fkCols.add(rs.getString("FKCOLUMN_NAME"));
      }
      // columns -> node fields
      List<DatabaseErDiagramTableFieldQuickResponse> fields = new ArrayList<>();
      try (ResultSet rs = meta.getColumns(null, null, tableName, "%")) {
        while (rs.next()) {
          String col = rs.getString("COLUMN_NAME");
          fields.add(
              new DatabaseErDiagramTableFieldQuickResponse(
                  col, rs.getString("TYPE_NAME"), pkCols.contains(col), fkCols.contains(col)));
        }
      }
      tables.add(new DatabaseErDiagramTableResponse(tableName, fields));

      // edges from this table's FKs
      try (ResultSet rs = meta.getImportedKeys(null, null, tableName)) {
        while (rs.next()) {
          relationships.add(
              new DatabaseErDiagramRelationResponse(
                  rs.getString("FKTABLE_NAME"), rs.getString("FKCOLUMN_NAME"),
                  rs.getString("PKTABLE_NAME"), rs.getString("PKCOLUMN_NAME")));
        }
      }
    }
    return new DatabaseErDiagramResponse(tables, relationships);
  }
}
