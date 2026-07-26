package com.database.resources.transfer;

import com.database.dto.request.transfer.DatabaseExportRequest;
import com.database.dto.request.transfer.DatabaseImportRequest;
import com.database.services.transfer.DatabaseTransferService;
import com.shared.dto.ApiResponse;
import com.shared.security.CurrentUser;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;

@Path("database/api/transfer")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DatabaseTransferResource {

  @Inject CurrentUser currentUser;

  @Inject DatabaseTransferService databaseTransferService;

  @POST
  @Path("export/")
  public Response exportDatabase(@Valid DatabaseExportRequest request) throws SQLException {
    return ApiResponse.success(
        databaseTransferService.exportDatabase(currentUser.getUser(), request), null);
  }

  @POST
  @Path("import/")
  public Response importDatabase(@Valid DatabaseImportRequest request) throws SQLException {
    return ApiResponse.success(
        databaseTransferService.importDatabase(currentUser.getUser(), request), null);
  }
}
