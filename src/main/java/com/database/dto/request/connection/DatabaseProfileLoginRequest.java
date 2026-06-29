package com.database.dto.request.connection;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public class DatabaseProfileLoginRequest {

  @NotBlank(message = "profile id is required")
  public UUID userId;

  public DatabaseProfileLoginRequest(UUID userId) {
    this.userId = userId;
  }
}
