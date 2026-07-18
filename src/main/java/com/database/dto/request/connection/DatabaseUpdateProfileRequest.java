package com.database.dto.request.connection;

import com.shared.enums.DatabaseEnvironment;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Partial update of a saved profile. Only {@code userId} is required; every
 * other field is optional and applied only when non-null (PATCH semantics).
 */
public class DatabaseUpdateProfileRequest {

  @NotNull(message = "userId is required field")
  public UUID userId;

  public DatabaseEnvironment environment;

  public Boolean readOnly;

  public String profileName;
}
