package com.database.resources.structure;

import com.database.services.structure.DatabaseTableStructureService;
import com.shared.dto.ApiResponse;
import com.shared.security.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

@Path("database/api/structure")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseTableStructureResource {

  @Inject CurrentUser currentUser;

  @Inject DatabaseTableStructureService databaseTableStructureService;

  @GET
  @Path("table-list-api/")
  public Response getDbTableListApi() throws SQLException {

    return ApiResponse.success(
        databaseTableStructureService.getDatabaseTableList(currentUser.getUser()), null);
  }
}
