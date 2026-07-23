package com.shared.config;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Native-image reflection registration.
 *
 * <p>REST methods return a raw {@code jakarta.ws.rs.core.Response} (built by {@code
 * ApiResponse.success/error}), so Quarkus cannot infer the serialized entity types at build time
 * and does not generate reflection metadata for them. In JVM mode Jackson uses runtime reflection
 * and it "just works"; in a GraalVM native image that metadata is absent, so Jackson fails with "No
 * serializer found for class ... no properties discovered".
 *
 * <p>Registering every DTO, entity and enum that crosses the JSON boundary here fixes that in one
 * place. Add new response/request DTOs to this list.
 */
@RegisterForReflection(
    targets = {
      // --- request DTOs ---
      com.database.dto.request.connection.DatabaseAddProfileRequest.class,
      com.database.dto.request.connection.DatabaseProfileLoginRequest.class,
      com.database.dto.request.connection.DatabaseProfileTestConnectionRequest.class,
      com.database.dto.request.connection.DatabaseTestConnectionRequest.class,
      com.database.dto.request.connection.DatabaseUpdateProfileRequest.class,
      com.database.dto.request.generic.DatabaseDbTableNameGenericRequest.class,
      com.database.dto.request.info.DatabaseDbRunQueryRequest.class,
      com.database.dto.request.info.DatabaseDbTableDataRequest.class,
      com.database.dto.request.info.DatabaseDbUserEntityRequest.class,
      com.database.dto.request.info.table_properties.DatabaseTableDetailResponse.class,
      com.database.dto.request.info.table_properties.DatabaseTableIndexResponse.class,
      com.database.dto.request.info.table_properties.DatabaseTableInfoResponse.class,
      com.database.dto.request.settings.DatabaseUpdatePasswordRequest.class,
      com.database.dto.request.transfer.DatabaseExportRequest.class,
      com.database.dto.request.transfer.DatabaseImportRequest.class,
      // --- response DTOs ---
      com.database.dto.response.connection.DatabaseLoginResponse.class,
      com.database.dto.response.connection.DatabaseTestConnectionResponse.class,
      com.database.dto.response.er_diagram.DatabaseErDiagramRelationResponse.class,
      com.database.dto.response.er_diagram.DatabaseErDiagramResponse.class,
      com.database.dto.response.er_diagram.DatabaseErDiagramTableFieldQuickResponse.class,
      com.database.dto.response.er_diagram.DatabaseErDiagramTableResponse.class,
      com.database.dto.response.info.DatabaseTableFieldsHelperListResponse.class,
      com.database.dto.response.info.DatabaseUserProfileResponse.class,
      com.database.dto.response.structure.DatabaseSchemaTableListResponse.class,
      com.database.dto.response.structure.DatabaseTableColumnMeta.class,
      com.database.dto.response.structure.DatabaseTableDataColumnDetailResponse.class,
      com.database.dto.response.structure.DatabaseTableDataResponse.class,
      com.database.dto.response.structure.DatabaseTableDependentTableResponse.class,
      com.database.dto.response.structure.DatabaseTableListResponse.class,
      com.database.dto.response.structure.DatabaseTablePropertiesColumnResource.class,
      com.database.dto.response.structure.run_query.DatabaseQueryResultResponse.class,
      com.database.dto.response.structure.run_query.DatabaseQueryResultResponse.ResultType.class,
      com.database.dto.response.transfer.DatabaseExportResponse.class,
      com.database.dto.response.transfer.DatabaseImportResponse.class,
      // --- entities ---
      com.database.model.DatabaseEntity.class,
      com.database.model.DbUserEntity.class,
      com.shared.model.BaseEntity.class,
      // --- shared DTOs + enums ---
      com.shared.dto.ApiResponse.class,
      com.shared.dto.JwtDecodePayloadDetails.class,
      com.shared.dto.PaginatedListResponse.class,
      com.shared.enums.DataBaseType.class,
      com.shared.enums.DatabaseConnectionStatus.class,
      com.shared.enums.DatabaseEnvironment.class,
    })
public class ReflectionConfig {}
