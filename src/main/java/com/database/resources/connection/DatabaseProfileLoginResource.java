package com.database.resources.connection;

import com.database.dto.request.connection.DatabaseProfileLoginRequest;
import com.database.services.connection.DatabaseProfileLoginService;
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
public class DatabaseProfileLoginResource {

  @Inject DatabaseProfileLoginService databaseProfileLoginService;

  @POST
  @Path("profile-login-api/")
  public Response profileLoginApi(@Valid DatabaseProfileLoginRequest request) {
    // this is an api to get jwt token for test connection and login with database
    // user
    return ApiResponse.success(databaseProfileLoginService.login(request), "Ok");
  }
}
