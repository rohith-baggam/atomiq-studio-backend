package com.shared.exceptions;

import com.shared.dto.ApiResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.sql.SQLException;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {

    if (exception instanceof ConflictException) {
      return ApiResponse.error(null, exception.getMessage(), Response.Status.CONFLICT);
    }
    if (exception instanceof UnauthorizedException) {
      return ApiResponse.error(null, exception.getMessage(), Response.Status.UNAUTHORIZED);
    }
    if (exception instanceof ResourceNotFoundException) {
      return ApiResponse.error(null, exception.getMessage(), Response.Status.NOT_FOUND);
    }

    if (exception instanceof ValidationException) {
      return ApiResponse.error(null, exception.getMessage(), Response.Status.BAD_REQUEST);
    }

    // Preserve JAX-RS/Quarkus-security status codes (401 for @Authenticated with no
    // token, 404, 405, …) instead of flattening them all to 400/500 below.
    if (exception instanceof WebApplicationException wae) {
      Response.StatusType status = wae.getResponse().getStatusInfo();
      return ApiResponse.error(null, exception.getMessage(), status);
    }

    // A DB error is a server/database failure, not a client (400) mistake — user
    // input is validated before it reaches SQL, so surface it as 500.
    if (exception instanceof SQLException) {
      return ApiResponse.error(null, exception.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
    }

    return ApiResponse.error(
        null, "An unexpected error occurred", Response.Status.INTERNAL_SERVER_ERROR);
  }
}
