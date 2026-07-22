package com.database.dto.request.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** Updates the stored password of a saved database profile. */
public class DatabaseUpdatePasswordRequest {

  @NotNull(message = "userId is required field")
  public UUID userId;

  @NotBlank(message = "password is required field")
  public String password;
}
