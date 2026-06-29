package com.database.repository;

import com.database.model.DatabaseEntity;
import com.shared.enums.DataBaseType;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DatabaseEntityRepository implements PanacheRepository<DatabaseEntity> {

  public DatabaseEntity findByNameAndType(String dbName, DataBaseType dbType) {
    return find("dbName = ?1 and dbType = ?2", dbName, dbType).firstResult();
  }
}
