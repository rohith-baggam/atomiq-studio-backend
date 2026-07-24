package com.shared.security;

import com.database.model.DbUserEntity;
import com.database.repository.DbUserEntityRepository;
import io.quarkus.security.UnauthorizedException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
public class AuthenticationFilter implements ContainerRequestFilter {
  @Inject JsonWebToken jwt;
  @Inject DbUserEntityRepository dbUserEntityRepository;
  @Inject CurrentUser currentUser;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    String subject = jwt.getSubject();
    if (subject == null) {
      return;
    }
    UUID userId;
    try {
      userId = UUID.fromString(subject);
    } catch (IllegalArgumentException e) {
      // A malformed subject is a bad token, not a server error — surface it as 401
      // rather than letting it fall through to the generic 500 mapper.
      throw new UnauthorizedException("Invalid token subject");
    }
    DbUserEntity dbUserEntity =
        dbUserEntityRepository
            .find("id", userId)
            .firstResultOptional()
            .orElseThrow(() -> new UnauthorizedException("User no longer active"));
    currentUser.setDbUserEntity(dbUserEntity);
  }
}
