package com.audit.utils.generate;

public record AuditTableGenerateColumnMeta(
    String name, String typeName, int size, int scale, int jdbcType, boolean nullable) {}
