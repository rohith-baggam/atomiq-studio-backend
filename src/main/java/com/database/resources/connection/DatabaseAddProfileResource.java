package com.database.resources.connection;

import com.database.dto.request.connection.DatabaseAddProfileRequest;
import com.database.dto.response.connection.DatabaseLoginResponse;
import com.database.services.connection.DatabaseAddProfileAndLoginService;
import com.shared.dto.ApiResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/connect/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseAddProfileResource {

  @Inject DatabaseAddProfileAndLoginService databaseAddProfileAndLoginService;

  @POST
  @Authenticated
  @Path("add-connection-api/")
  public Response addConnectionApi(@BeanParam DatabaseAddProfileRequest request) {
    // this is an api for adding new database profile and test before connecting
    DatabaseLoginResponse databaseLoginResponse =
        databaseAddProfileAndLoginService.addDatabaseProfileAndLogin(request);
    return ApiResponse.success(databaseLoginResponse, "Connected successfully");
  }
}
