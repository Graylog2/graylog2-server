/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.rest.bulk.AuditParams;
import org.graylog2.rest.bulk.SequentialBulkExecutor;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Actions on any number of indices, in Graylog's bulk format: one audit event and, on failure, one explanation per
 * index. Close, open and delete work on any index except a current write index and system indices (names starting
 * with a dot). Close and delete go through Graylog's {@link Indices} service, like its own endpoints, so index ranges
 * stay consistent; open is Graylog's reopen (retention skips the index afterwards) for indices Graylog manages, and a
 * plain open for the others. Flush, clear cache and force merge work on any open index. Rotating is Graylog's own
 * {@code /system/deflector/bulk_cycle}.
 */
@RequiresAuthentication
@Tag(name = "Indexer/Indices/Management/Actions", description = "Index Management: actions on indices")
@Path("/system/indexer/management/indices/actions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IndexActionsResource extends RestResource {
    /** Actions run one index after another within the request. */
    static final int MAX_INDICES = 1000;
    private static final String STATUS_CLOSED = "close";
    private static final String AUDIT_ENTITY_ID = "index";

    private final IndexHealthService indexHealthService;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final SystemJobManager systemJobManager;
    private final AuditEventSender auditEventSender;
    private final ObjectMapper objectMapper;

    @Inject
    public IndexActionsResource(IndexHealthService indexHealthService,
                                IndexSetRegistry indexSetRegistry,
                                Indices indices,
                                SystemJobManager systemJobManager,
                                AuditEventSender auditEventSender,
                                ObjectMapper objectMapper) {
        this.indexHealthService = indexHealthService;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.systemJobManager = systemJobManager;
        this.auditEventSender = auditEventSender;
        this.objectMapper = objectMapper;
    }

    @POST
    @Timed
    @Path("/close")
    @Operation(summary = "Close indices (not a current write index or a system index).")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse close(@Parameter(name = "Indices to close", required = true) @NotNull BulkOperationRequest request,
                                       @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_CLOSE, RestPermissions.INDICES_CHANGESTATE, Scope.NOT_WRITE_INDEX, row -> {
            if (!STATUS_CLOSED.equals(row.status())) {
                indices.close(row.index());
            }
        });
    }

    @POST
    @Timed
    @Path("/open")
    @Operation(summary = "Open closed indices (not system indices). For indices Graylog manages this is Graylog's own "
            + "reopen, so retention then skips them; other indices are opened as they are.")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse open(@Parameter(name = "Indices to open", required = true) @NotNull BulkOperationRequest request,
                                      @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_OPEN, RestPermissions.INDICES_CHANGESTATE, Scope.ANY, row -> {
            if (!STATUS_CLOSED.equals(row.status())) {
                return;
            }
            if (indexSetRegistry.isManagedIndex(row.index())) {
                indices.reopenIndex(row.index());
            } else {
                // Not Graylog's: no reopened marker (a Graylog alias) on someone else's index.
                indexHealthService.openIndex(row.index());
            }
        });
    }

    @POST
    @Timed
    @Path("/delete")
    @Operation(summary = "Delete indices (not a current write index or a system index).")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse delete(@Parameter(name = "Indices to delete", required = true) @NotNull BulkOperationRequest request,
                                        @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_DELETE, RestPermissions.INDICES_DELETE, Scope.NOT_WRITE_INDEX,
                row -> indices.delete(row.index()));
    }

    @POST
    @Timed
    @Path("/flush")
    @Operation(summary = "Flush open indices.")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse flush(@Parameter(name = "Indices to flush", required = true) @NotNull BulkOperationRequest request,
                                       @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_FLUSH, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN,
                row -> indices.flush(row.index()));
    }

    @POST
    @Timed
    @Path("/clear_cache")
    @Operation(summary = "Clear the caches of open indices.")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse clearCache(@Parameter(name = "Indices whose caches to clear", required = true) @NotNull BulkOperationRequest request,
                                            @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_CLEAR_CACHE, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN,
                row -> indexHealthService.clearCache(row.index()));
    }

    @POST
    @Timed
    @Path("/force_merge")
    @Operation(summary = "Force-merge open indices, as Graylog system jobs (System > Overview).")
    @NoAuditEvent("Audit events triggered manually")
    public BulkOperationResponse forceMerge(@Parameter(name = "Indices to force merge", required = true) @NotNull BulkOperationRequest request,
                                            @Context UserContext userContext) {
        return run(request, userContext, AuditEventTypes.ES_INDEX_FORCE_MERGE, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN, row -> {
            // The index set's own optimization setting; 1 segment for indices outside Graylog.
            final int maxNumSegments = indexSetRegistry.getForIndex(row.index())
                    .map(set -> set.getConfig().indexOptimizationMaxNumSegments())
                    .orElse(1);
            systemJobManager.submit(OptimizeIndexJob.forIndex(row.index(), maxNumSegments));
        });
    }

    private enum Scope {
        /** Any index but system indices. */
        ANY,
        /** Any index but system indices and current write indices. */
        NOT_WRITE_INDEX,
        /** Any open index. */
        ANY_OPEN
    }

    @FunctionalInterface
    private interface Action {
        void apply(CatIndex row) throws Exception;
    }

    // As OutdatedIndex#isSystemIndex: OpenSearch's and its plugins' own indices start with a dot.
    private static boolean isSystemIndex(String index) {
        return index.startsWith(".");
    }

    private BulkOperationResponse run(BulkOperationRequest request, UserContext userContext, String auditEventType,
                                      String permission, Scope scope, Action action) {
        if (request.entityIds() != null && request.entityIds().size() > MAX_INDICES) {
            throw new BadRequestException(f("At most %d indices per request", MAX_INDICES));
        }
        // Only exact names of existing indices get through: no wildcards, comma lists or aliases.
        final Map<String, CatIndex> existing = indexHealthService.indices().stream()
                .filter(row -> row.index() != null)
                .collect(Collectors.toMap(CatIndex::index, Function.identity(), (a, b) -> a));

        final SequentialBulkExecutor<CatIndex, UserContext> executor = new SequentialBulkExecutor<>((index, context) -> {
            final CatIndex row = checked(index, existing.get(index), permission, scope);
            action.apply(row);
            return row;
        }, auditEventSender, objectMapper);
        return executor.executeBulkOperation(request, userContext, new AuditParams(auditEventType, AUDIT_ENTITY_ID, CatIndex.class));
    }

    private CatIndex checked(String index, CatIndex row, String permission, Scope scope) {
        // Permission first, so the answer doesn't tell a user which index names exist.
        if (!isPermitted(permission, index)) {
            throw new ForbiddenException(f("Not permitted (needs %s)", permission));
        }
        if (row == null) {
            throw new NotFoundException("No such index");
        }
        if (scope != Scope.ANY_OPEN && isSystemIndex(index)) {
            throw new BadRequestException("System index (name starts with a dot); OpenSearch or a plugin owns it");
        }
        if (scope == Scope.NOT_WRITE_INDEX && indexSetRegistry.isCurrentWriteIndex(index)) {
            throw new BadRequestException("Current write index; rotate the index set first");
        }
        if (scope == Scope.ANY_OPEN && STATUS_CLOSED.equals(row.status())) {
            throw new BadRequestException("Index is closed");
        }
        return row;
    }
}
