package com.database.resources.helper;

import com.database.dto.request.generic.DatabaseDbTableNameGenericRequest;
import com.database.services.structure.DatabaseTableStructureService;
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

@Path("database/api/helper-list/")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseTableFieldsHelperListResource {

  @Inject DatabaseTableStructureService databaseTableStructureService;

  @Inject CurrentUser currentUser;

  @GET
  @Path("table-fields-helper-list-api/")
  public Response tableFieldHelperListApi(@BeanParam DatabaseDbTableNameGenericRequest request)
      throws SQLException {
    return ApiResponse.success(
        databaseTableStructureService.tableFieldHelperList(currentUser.getUser(), request), null);
  }
}
