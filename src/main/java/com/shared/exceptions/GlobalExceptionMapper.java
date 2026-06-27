package com.shared.exceptions;

import com.shared.dto.ApiResponse;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  @Override
  public Response toResponse(Exception exception) {

    System.out.println(exception);
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

    return ApiResponse.error(null, "An unexpected error occurred", Response.Status.BAD_REQUEST);
  }
}
