package com.shared.dto;

public class PaginatedListResponse<T> {

  public long count;
  public boolean hasNext;
  public boolean hasPrev;
  public T results;

  public PaginatedListResponse(long count, long limit, long offset, T results) {

    this.count = count;
    this.hasNext = offset + limit < count;
    this.hasPrev = offset > 0;
    this.results = results;
  }
}
