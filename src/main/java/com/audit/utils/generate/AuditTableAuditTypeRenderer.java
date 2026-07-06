package com.audit.utils.generate;

public final class AuditTableAuditTypeRenderer {

  private AuditTableAuditTypeRenderer() {}

  /**
   * Only types that actually carry a length/precision modifier get one. Driven by the reported type
   * name rather than the JDBC type code: drivers (e.g. the Postgres JDBC driver) report unbounded
   * types like {@code text} under the same JDBC code as {@code varchar}, so keying off {@code
   * jdbcType()} alone previously emitted {@code text(2147483647)}.
   */
  public static String renderTypeDefault(AuditTableGenerateColumnMeta c) {
    String type = c.typeName();
    String normalized = type.toLowerCase();
    if (normalized.contains("char") && c.size() > 0 && c.size() < Integer.MAX_VALUE) {
      return type + "(" + c.size() + ")";
    }
    if ((normalized.equals("numeric") || normalized.equals("decimal")) && c.size() > 0) {
      return c.scale() > 0
          ? type + "(" + c.size() + "," + c.scale() + ")"
          : type + "(" + c.size() + ")";
    }
    return type;
  }
}
