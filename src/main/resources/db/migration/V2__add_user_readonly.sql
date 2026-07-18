-- Read-only flag per saved profile. When true, the run-query connection is
-- opened read-only so writes/DDL are rejected by the target engine.
ALTER TABLE users ADD COLUMN "readOnly" BOOLEAN NOT NULL DEFAULT 0;
