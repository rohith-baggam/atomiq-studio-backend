package com.audit.resources;

import com.audit.dto.request.AuditExecuteSchemaRequest;
import com.audit.dto.request.AuditGenerateSchemaRequest;
import com.audit.services.AuditTableConfigService;
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

@Path("audit/api/config/")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuditTableConfigResources {

  @Inject CurrentUser currentUser;
  @Inject AuditTableConfigService auditTableConfigService;

  @POST
  @Path("generate-audit-create-schema-api/")
  public Response generateAuditCreateSchemaApi(@Valid AuditGenerateSchemaRequest request)
      throws SQLException {
    return ApiResponse.success(
        auditTableConfigService.generateAuditCreateSchemaApi(request, currentUser.getUser()), null);
  }

  @POST
  @Path("execute-audit-create-schema-api/")
  public Response executeAuditCreateSchemaApi(@Valid AuditExecuteSchemaRequest request)
      throws SQLException {
    return ApiResponse.success(
        auditTableConfigService.executeAuditCreateSchemaApi(request, currentUser.getUser()), null);
  }
}
