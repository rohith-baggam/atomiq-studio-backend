package com.database.resources.connection;

import com.database.dto.request.connection.DatabaseProfileTestConnectionRequest;
import com.database.services.connection.DatabaseProfileTestConnectionService;
import com.shared.dto.ApiResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/connect/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseProfileTestConnectionResource {

  @Inject DatabaseProfileTestConnectionService databaseProfileTestConnectionService;

  @POST
  @Path("profile-connection-status-api/")
  public Response profileConnectionStatusApi(@Valid DatabaseProfileTestConnectionRequest request) {
    // this is an api to get connection status of database user profile
    return ApiResponse.success(databaseProfileTestConnectionService.connect(request), "Ok");
  }
}
