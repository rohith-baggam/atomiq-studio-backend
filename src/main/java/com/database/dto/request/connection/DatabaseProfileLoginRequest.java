package com.database.dto.request.connection;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class DatabaseProfileLoginRequest {

  @NotNull(message = "userId is required field")
  public UUID userId;

  public DatabaseProfileLoginRequest(UUID userId) {
    this.userId = userId;
  }
}
