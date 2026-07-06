package com.audit.utils.generate;

import java.util.List;
import java.util.Set;

public record AuditTableGenerateTableSchema(
    String schema,
    String table,
    List<AuditTableGenerateColumnMeta> columns,
    Set<String> primaryKeys) {}
