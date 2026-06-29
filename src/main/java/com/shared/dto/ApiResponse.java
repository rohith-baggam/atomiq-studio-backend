package com.shared.dto;

import jakarta.ws.rs.core.Response;

public class ApiResponse<T> {

  public String message;
  public T results;

  public ApiResponse() {}

  public static <T> Response success(T results, String message) {
    ApiResponse<T> result = new ApiResponse<>();
    result.message = message;
    result.results = results;
    return Response.status(Response.Status.OK).entity(result).build();
  }

  public static <T> Response error(T results, String message, Response.Status status) {
    ApiResponse<T> result = new ApiResponse<>();
    result.message = message;
    result.results = results;
    if (status == null) {
      status = Response.Status.BAD_REQUEST;
    }
    return Response.status(status).entity(result).build();
  }
}
