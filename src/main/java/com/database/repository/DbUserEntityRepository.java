package com.database.repository;

import com.database.model.DatabaseEntity;
import com.database.model.DbUserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class DbUserEntityRepository implements PanacheRepository<DbUserEntity> {

  public Optional<DbUserEntity> findByDbUserId(UUID id) {
    return find("id", id).firstResultOptional();
  }

  public Optional<DbUserEntity> findByProfileName(String profileName) {
    return find("profileName", profileName).firstResultOptional();
  }

  public Optional<DbUserEntity> findByDbEntityNameTypeAndUsername(
      DatabaseEntity databaseEntity, String username) {
    return find("databaseEntity =?1 and username = ?2", databaseEntity, username)
        .firstResultOptional();
  }
}
