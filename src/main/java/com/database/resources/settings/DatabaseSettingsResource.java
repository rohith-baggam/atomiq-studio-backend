package com.database.resources.settings;

import com.database.dto.request.settings.DatabaseUpdatePasswordRequest;
import com.database.services.settings.DatabaseSettingsService;
import com.shared.dto.ApiResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/settings/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseSettingsResource {

  @Inject DatabaseSettingsService databaseSettingsService;

  @PATCH
  @Path("update-password-api")
  @Authenticated
  public Response updatePasswordApi(@Valid DatabaseUpdatePasswordRequest request) {
    databaseSettingsService.updatePassword(request);
    return ApiResponse.success(null, "Password updated successfully");
  }
}
