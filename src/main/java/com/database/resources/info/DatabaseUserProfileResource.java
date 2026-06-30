package com.database.resources.info;

import com.database.dto.request.connection.DatabaseProfileTestConnectionRequest;
import com.database.services.info.DatabaseUserProfileService;
import com.shared.dto.ApiResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/profile/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseUserProfileResource {

  @Inject DatabaseUserProfileService databaseUserProfileService;

  @GET
  @Path("db-profile-list-api/")
  public Response getProfileListApi() {
    return ApiResponse.success(databaseUserProfileService.getDbUserEntityList(), "ok");
  }

  @GET
  @Path("me/")
  @Authenticated
  public Response me() {
    return ApiResponse.success(databaseUserProfileService.entityDetails(), null);
  }

  @DELETE
  @Path("delete-profile-api")
  public Response deleteProfileApi(DatabaseProfileTestConnectionRequest request) {
    databaseUserProfileService.deleteDbUser(request);
    return ApiResponse.success(null, "Deleted successfully");
  }
}
