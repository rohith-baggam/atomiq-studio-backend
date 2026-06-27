package com.database.model;

import com.shared.enums.DataBaseType;
import com.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "database",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_database_name_type",
          columnNames = {"databaseName", "databaseType"})
    })
public class DatabaseEntity extends BaseEntity {

  @Column(name = "databaseName", length = 128, nullable = false)
  public String dbName;

  @Enumerated(EnumType.STRING)
  @Column(name = "databaseType", length = 128, nullable = false)
  public DataBaseType dbType;
}
