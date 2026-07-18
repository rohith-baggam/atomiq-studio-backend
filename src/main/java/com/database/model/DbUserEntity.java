package com.database.model;

import com.shared.enums.DatabaseConnectionStatus;
import com.shared.enums.DatabaseEnvironment;
import com.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class DbUserEntity extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "databaseId", nullable = false)
  public DatabaseEntity databaseEntity;

  @Column(name = "profileName", length = 128, nullable = false)
  public String profileName;

  @Column(name = "username", length = 128, nullable = false)
  public String username;

  @Column(name = "host", length = 128, nullable = false)
  public String host;

  @Column(name = "port", nullable = false)
  public Integer port;

  @Column(name = "password", nullable = false)
  public String password;

  @Column(name = "environment")
  @Enumerated(EnumType.STRING)
  public DatabaseEnvironment environment = DatabaseEnvironment.DEV;

  @Column(name = "readOnly", nullable = false)
  public Boolean readOnly = false;

  @Column(name = "lastConnectedTime")
  public LocalDateTime lastConnectedTime;

  @Column(name = "lastConnectionStatus")
  @Enumerated(EnumType.STRING)
  public DatabaseConnectionStatus lastConnectionStatus;
}
