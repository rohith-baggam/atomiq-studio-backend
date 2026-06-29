CREATE TABLE "database" (
    id TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    "databaseName" VARCHAR(128) NOT NULL,
    "databaseType" VARCHAR(128) NOT NULL CHECK ("databaseType" IN ('POSTGRES','MYSQL','MSSQL','ORACLE')),
    PRIMARY KEY (id),
    CONSTRAINT uk_database_name_type UNIQUE ("databaseName", "databaseType")
);


CREATE TABLE users (
    id TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    "databaseId" TEXT NOT NULL,
    "profileName" VARCHAR(128) NOT NULL,
    username VARCHAR(128) NOT NULL,
    host VARCHAR(128) NOT NULL,
    port INTEGER NOT NULL,
    password VARCHAR(255) NOT NULL,
    environment VARCHAR(255) DEFAULT 'DEV' CHECK (environment IN ('LOCAL','DEV','STAGE','PROD')),
    "lastConnectedTime" TIMESTAMP,
    "lastConnectionStatus" VARCHAR(255) CHECK ("lastConnectionStatus" IN ('CONNECTED','HEALTHY','FAILED')),
    PRIMARY KEY (id),
    CONSTRAINT fk_users_database FOREIGN KEY ("databaseId") REFERENCES "database"(id)
);
