package com.shared.security;

import com.database.model.DbUserEntity;
import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentUser {

  private DbUserEntity dbEntity;

  public DbUserEntity getUser() {
    return dbEntity;
  }

  public void setDbUserEntity(DbUserEntity dbEntity) {
    this.dbEntity = dbEntity;
  }
}
