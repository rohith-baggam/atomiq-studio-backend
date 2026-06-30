package com.database.resources.structure;

import com.database.dto.request.generic.DatabaseDbTableNameGenericRequest;
import com.database.services.structure.DatabaseTableDependencyService;
import com.shared.dto.ApiResponse;
import com.shared.security.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

@Path("database/api/dependency/")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseTableDependencyResources {

  @Inject DatabaseTableDependencyService databaseTableDependencyService;

  @Inject CurrentUser currentUser;

  @GET
  @Path("table-dependency-api/")
  public Response getdependentApi(@BeanParam DatabaseDbTableNameGenericRequest request)
      throws SQLException {
    return ApiResponse.success(
        databaseTableDependencyService.getDependencyTables(request, currentUser.getUser()), null);
  }

  @GET
  @Path("db-er-diagram-api/")
  public Response getDbErDiagram() throws SQLException {
    return ApiResponse.success(
        databaseTableDependencyService.getDbErDiagram(currentUser.getUser()), null);
  }
}
