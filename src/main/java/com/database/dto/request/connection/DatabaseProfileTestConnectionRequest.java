package com.database.dto.request.connection;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

// When database is already saved and test with id
public class DatabaseProfileTestConnectionRequest {
  @NotNull(message = "userId is required field")
  public UUID userId;
}
