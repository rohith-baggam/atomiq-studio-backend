package com.database.dto.response.structure;

public class DatabaseTablePropertiesColumnResource {

  public String columnName;
  public String dataType;
  public Integer columnSize;
  public Integer decimalDigits;
  public Integer ordinalPosition;
  public boolean notNull;
  public String defaultValue;
  public String identity;
  public String comments;
  public boolean primaryKey;
  public boolean foreignKey;
  public boolean indexed;
  public boolean unique;

  public DatabaseTablePropertiesColumnResource(
      String columnName,
      String dataType,
      Integer columnSize,
      Integer decimalDigits,
      Integer ordinalPosition,
      boolean notNull,
      String defaultValue,
      String identity,
      String comments,
      boolean primaryKey,
      boolean foreignKey,
      boolean indexed,
      boolean unique) {

    this.columnName = columnName;
    this.dataType = dataType;
    this.columnSize = columnSize;
    this.decimalDigits = decimalDigits;
    this.ordinalPosition = ordinalPosition;
    this.notNull = notNull;
    this.defaultValue = defaultValue;
    this.identity = identity;
    this.comments = comments;
    this.primaryKey = primaryKey;
    this.foreignKey = foreignKey;
    this.indexed = indexed;
    this.unique = unique;
  }
}
