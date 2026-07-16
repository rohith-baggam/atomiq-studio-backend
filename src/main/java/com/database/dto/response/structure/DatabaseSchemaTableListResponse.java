package com.database.dto.response.structure;

import java.util.List;

public class DatabaseSchemaTableListResponse {

    public String schemaName;
    public List<DatabaseTableListResponse> tables;

    public DatabaseSchemaTableListResponse(
            String schemaName,
            List<DatabaseTableListResponse> tables) {
        this.schemaName = schemaName;
        this.tables = tables;
    }
}
