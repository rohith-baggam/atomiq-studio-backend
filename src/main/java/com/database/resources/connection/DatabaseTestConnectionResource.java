package com.database.resources.connection;

import com.database.dto.request.connection.DatabaseTestConnectionRequest;
import com.database.dto.response.connection.DatabaseTestConnectionResponse;
import com.database.services.connection.DatabaseTestConnectionService;
import com.shared.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/connect/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseTestConnectionResource {
  @Inject DatabaseTestConnectionService databaseTestConnectionService;

  @POST
  @Path("test-database-api/")
  public Response connectDatabase(DatabaseTestConnectionRequest request) {
    // this is an api just to check database details and connection status without
    // saving to saving. This is api has no interaction with database
    DatabaseTestConnectionResponse databaseTestConnectionResponse =
        databaseTestConnectionService.testConnect(request);

    return ApiResponse.success(databaseTestConnectionResponse, "connected");
  }
}
