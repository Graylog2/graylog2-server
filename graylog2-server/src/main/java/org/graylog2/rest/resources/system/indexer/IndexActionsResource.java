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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.management.CatIndex;
import org.graylog2.indexer.management.IndexActionRequest;
import org.graylog2.indexer.management.IndexActionResult;
import org.graylog2.indexer.management.IndexHealthService;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.security.RestrictToLeader;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Per-index and bulk actions. Close, open and delete work on any index except a current write index and system
 * indices (names starting with a dot). Close and
 * delete go through Graylog's {@link Indices} service, like its own endpoints, so index ranges stay consistent; open
 * is Graylog's reopen (retention skips the index afterwards) for indices Graylog manages, and a plain OpenSearch open
 * for the others. Flush, clear cache and force merge work on any open index.
 */
@RequiresAuthentication
@Tag(name = "Indexer/Indices/Management/Actions", description = "Index Management: actions on indices")
@Path("/system/indexer/management/indices/actions")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IndexActionsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexActionsResource.class);
    private static final String STATUS_CLOSED = "close";

    private final IndexHealthService indexHealthService;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final SystemJobManager systemJobManager;
    private final ActivityWriter activityWriter;

    @Inject
    public IndexActionsResource(IndexHealthService indexHealthService,
                                IndexSetRegistry indexSetRegistry,
                                Indices indices,
                                SystemJobManager systemJobManager,
                                ActivityWriter activityWriter) {
        this.indexHealthService = indexHealthService;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.systemJobManager = systemJobManager;
        this.activityWriter = activityWriter;
    }

    @POST
    @Timed
    @Path("/close")
    @Operation(summary = "Close indices (not a current write index).")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_CLOSE)
    public IndexActionResult.Response close(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_CHANGESTATE, Scope.NOT_WRITE_INDEX, (index, row) -> {
            if (STATUS_CLOSED.equals(row.status())) {
                return "already closed";
            }
            indices.close(index);
            return "closed";
        });
    }

    @POST
    @Timed
    @Path("/open")
    @Operation(summary = "Open closed indices. For indices Graylog manages this is Graylog's own reopen, so retention "
            + "then skips them; other indices are opened as they are.")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_OPEN)
    public IndexActionResult.Response open(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_CHANGESTATE, Scope.ANY, (index, row) -> {
            if (!STATUS_CLOSED.equals(row.status())) {
                return "already open";
            }
            if (indexSetRegistry.isManagedIndex(index)) {
                indices.reopenIndex(index);
                return "reopened (retention will skip it)";
            }
            // Not Graylog's: no reopened marker (a Graylog alias) on someone else's index.
            indexHealthService.openIndex(index);
            return "opened";
        });
    }

    @POST
    @Timed
    @Path("/delete")
    @Operation(summary = "Delete indices (not a current write index).")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_DELETE)
    public IndexActionResult.Response delete(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_DELETE, Scope.NOT_WRITE_INDEX, (index, row) -> {
            indices.delete(index);
            return "deleted";
        });
    }

    @POST
    @Timed
    @Path("/flush")
    @Operation(summary = "Flush open indices.")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_FLUSH)
    public IndexActionResult.Response flush(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN, (index, row) -> {
            indices.flush(index);
            return "flushed";
        });
    }

    @POST
    @Timed
    @Path("/clear_cache")
    @Operation(summary = "Clear the caches of open indices.")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_CLEAR_CACHE)
    public IndexActionResult.Response clearCache(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN, (index, row) -> {
            indexHealthService.clearCache(index);
            return "cache cleared";
        });
    }

    @POST
    @Timed
    @Path("/force_merge")
    @Operation(summary = "Force-merge open indices, as Graylog system jobs (System > Overview).")
    @AuditEvent(type = AuditEventTypes.ES_INDEX_FORCE_MERGE)
    public IndexActionResult.Response forceMerge(@Valid @NotNull IndexActionRequest request) {
        return run(request, RestPermissions.INDICES_CHANGESTATE, Scope.ANY_OPEN, (index, row) -> {
            // The index set's own optimization setting; 1 segment for indices outside Graylog.
            final int maxNumSegments = indexSetRegistry.getForIndex(index)
                    .map(set -> set.getConfig().indexOptimizationMaxNumSegments())
                    .orElse(1);
            systemJobManager.submit(OptimizeIndexJob.forIndex(index, maxNumSegments));
            return f("force merge to %d segment(s) queued as a system job", maxNumSegments);
        });
    }

    @POST
    @Timed
    @Path("/rotate")
    @RestrictToLeader
    @Operation(summary = "Rotate the index sets of the given current write indices (a new write index per set), "
            + "as Graylog's own deflector cycle does.")
    @AuditEvent(type = AuditEventTypes.ES_WRITE_INDEX_UPDATE_JOB_START)
    public IndexActionResult.Response rotate(@Valid @NotNull IndexActionRequest request) {
        final Map<String, CatIndex> existing = existingIndices();
        // Rotating is per index set: a bulk request rotates each set once.
        final Set<String> rotatedSets = new HashSet<>();

        return new IndexActionResult.Response(request.indices().stream()
                .distinct()
                .map(index -> {
                    if (!existing.containsKey(index)) {
                        return IndexActionResult.failed(index, "no such index");
                    }
                    // Unscoped, as in Graylog's DeflectorResource.
                    if (!isPermitted(RestPermissions.DEFLECTOR_CYCLE)) {
                        return IndexActionResult.failed(index, f("not permitted (needs %s)", RestPermissions.DEFLECTOR_CYCLE));
                    }
                    try {
                        final Optional<IndexSet> indexSet = indexSetRegistry.getForIndex(index);
                        if (indexSet.isEmpty()) {
                            return IndexActionResult.failed(index, "not managed by Graylog");
                        }
                        if (!indexSetRegistry.isCurrentWriteIndex(index)) {
                            return IndexActionResult.failed(index, "not a current write index; only write indices can be rotated");
                        }
                        final IndexSetConfig config = indexSet.get().getConfig();
                        if (!config.isWritable()) {
                            return IndexActionResult.failed(index, f("index set <%s> is not writable", config.title()));
                        }
                        if (!rotatedSets.add(config.id())) {
                            return IndexActionResult.ok(index, f("index set <%s> already rotated by this request", config.title()));
                        }

                        final String msg = f("Cycling deflector for index set <%s>. Reason: REST request (Index Management).", config.id());
                        LOG.info(msg);
                        activityWriter.write(new Activity(msg, IndexActionsResource.class));
                        indexSet.get().cycle();
                        return IndexActionResult.ok(index, f("rotated: index set <%s> now writes to a new index", config.title()));
                    } catch (Exception e) {
                        LOG.warn("Rotating the index set of <{}> failed", index, e);
                        return IndexActionResult.failed(index, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                    }
                })
                .toList());
    }

    private enum Scope {
        /** Any index. */
        ANY,
        /** Any index except a current write index. */
        NOT_WRITE_INDEX,
        /** Any open index. */
        ANY_OPEN
    }

    // As OutdatedIndex#isSystemIndex: OpenSearch's and its plugins' own indices start with a dot.
    private static boolean isSystemIndex(String index) {
        return index.startsWith(".");
    }

    @FunctionalInterface
    private interface Action {
        String apply(String index, CatIndex row) throws Exception;
    }

    // Only exact names of existing indices get through: no wildcards, comma lists or aliases.
    private Map<String, CatIndex> existingIndices() {
        return indexHealthService.indices().stream()
                .filter(row -> row.index() != null)
                .collect(Collectors.toMap(CatIndex::index, Function.identity(), (a, b) -> a));
    }

    private IndexActionResult.Response run(IndexActionRequest request, String permission, Scope scope, Action action) {
        final Map<String, CatIndex> existing = existingIndices();

        return new IndexActionResult.Response(request.indices().stream()
                .distinct()
                .map(index -> runOne(index, existing.get(index), permission, scope, action))
                .toList());
    }

    private IndexActionResult runOne(String index, CatIndex row, String permission, Scope scope, Action action) {
        // Permission first, so the answer doesn't tell a user which index names exist.
        if (!isPermitted(permission, index)) {
            return IndexActionResult.failed(index, f("not permitted (needs %s)", permission));
        }
        if (row == null) {
            return IndexActionResult.failed(index, "no such index");
        }
        try {
            if (scope != Scope.ANY_OPEN && isSystemIndex(index)) {
                return IndexActionResult.failed(index, "system index (name starts with a dot); OpenSearch or a plugin owns it");
            }
            if (scope == Scope.NOT_WRITE_INDEX && indexSetRegistry.isCurrentWriteIndex(index)) {
                return IndexActionResult.failed(index, "current write index; rotate the index set first");
            }
            if (scope == Scope.ANY_OPEN && STATUS_CLOSED.equals(row.status())) {
                return IndexActionResult.failed(index, "index is closed");
            }
            return IndexActionResult.ok(index, action.apply(index, row));
        } catch (Exception e) {
            LOG.warn("Index action on <{}> failed", index, e);
            return IndexActionResult.failed(index, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
