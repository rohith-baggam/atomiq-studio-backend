package com.shared.utils;

import com.database.model.DbUserEntity;
import io.smallrye.jwt.build.Jwt;
import java.time.Duration;

public class JwtUtils {

  public static String generateJwt(DbUserEntity dbUserEntity) {

    String jwtToken =
        Jwt.issuer("blog-app")
            .subject(dbUserEntity.id.toString())
            .claim("id", dbUserEntity.id)
            .claim("dbName", dbUserEntity.databaseEntity.dbName)
            .claim("dbType", dbUserEntity.databaseEntity.dbType)
            .claim("username", dbUserEntity.username)
            .claim("host", dbUserEntity.host)
            .claim("port", dbUserEntity.port)
            .expiresIn(Duration.ofDays(7))
            .sign();
    return jwtToken;
  }
}
