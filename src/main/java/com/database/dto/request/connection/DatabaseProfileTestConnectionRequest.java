package com.database.dto.request.connection;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

// When database is already saved and test with id
public class DatabaseProfileTestConnectionRequest {
  @NotBlank(message = "profile id is required")
  public UUID userId;
}
