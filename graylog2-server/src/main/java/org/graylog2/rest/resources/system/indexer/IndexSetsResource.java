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
import com.mongodb.DuplicateKeyException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.IndexSetStatsCreator;
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.indexer.indices.stats.IndexStatistics;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@RequiresAuthentication
@Api(value = "System/IndexSets", description = "Index sets")
@Path("/system/indices/index_sets")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetsResource.class);

    private final Indices indices;
    private final IndexSetService indexSetService;
    private final IndexSetRegistry indexSetRegistry;
    private final IndexSetValidator indexSetValidator;
    private final IndexSetCleanupJob.Factory indexSetCleanupJobFactory;
    private final IndexSetStatsCreator indexSetStatsCreator;
    private final ClusterConfigService clusterConfigService;
    private final SystemJobManager systemJobManager;

    @Inject
    public IndexSetsResource(final Indices indices,
                             final IndexSetService indexSetService,
                             final IndexSetRegistry indexSetRegistry,
                             final IndexSetValidator indexSetValidator,
                             final IndexSetCleanupJob.Factory indexSetCleanupJobFactory,
                             final IndexSetStatsCreator indexSetStatsCreator,
                             final ClusterConfigService clusterConfigService,
                             final SystemJobManager systemJobManager) {
        this.indices = requireNonNull(indices);
        this.indexSetService = requireNonNull(indexSetService);
        this.indexSetRegistry = indexSetRegistry;
        this.indexSetValidator = indexSetValidator;
        this.indexSetCleanupJobFactory = requireNonNull(indexSetCleanupJobFactory);
        this.indexSetStatsCreator = indexSetStatsCreator;
        this.clusterConfigService = clusterConfigService;
        this.systemJobManager = systemJobManager;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get a list of all index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
    })
    public IndexSetResponse list(@ApiParam(name = "skip", value = "The number of elements to skip (offset).", required = true)
                                 @QueryParam("skip") @DefaultValue("0") int skip,
                                 @ApiParam(name = "limit", value = "The maximum number of elements to return.", required = true)
                                 @QueryParam("limit") @DefaultValue("0") int limit,
                                 @ApiParam(name = "stats", value = "Include index set stats.")
                                 @QueryParam("stats") @DefaultValue("false") boolean computeStats) {
        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();

        List<IndexSetSummary> indexSets;
        int count;

        if (limit > 0) {
            // First collect all index set ids the user is allowed to see.
            final Set<String> allowedIds = indexSetService.findAll().stream()
                    .filter(indexSet -> isPermitted(RestPermissions.INDEXSETS_READ, indexSet.id()))
                    .map(IndexSetConfig::id)
                    .collect(Collectors.toSet());

            indexSets = indexSetService.findPaginated(allowedIds, limit, skip).stream()
                    .map(config -> IndexSetSummary.fromIndexSetConfig(config, config.equals(defaultIndexSet)))
                    .collect(Collectors.toList());
            count = allowedIds.size();
        } else {
            indexSets = indexSetService.findAll().stream()
                    .filter(indexSetConfig -> isPermitted(RestPermissions.INDEXSETS_READ, indexSetConfig.id()))
                    .map(config -> IndexSetSummary.fromIndexSetConfig(config, config.equals(defaultIndexSet)))
                    .collect(Collectors.toList());
            count = indexSets.size();
        }

        final Map<String, IndexSetStats> stats;
        if (computeStats) {
            stats = indexSetRegistry.getAll().stream()
                    .collect(Collectors.toMap(indexSet -> indexSet.getConfig().id(), indexSetStatsCreator::getForIndexSet));
        } else {
            stats = Collections.emptyMap();
        }

        return IndexSetResponse.create(count, indexSets, stats);
    }

    @GET
    @Path("stats")
    @Timed
    @ApiOperation(value = "Get stats of all index sets")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
    })
    public IndexSetStats globalStats() {
        checkPermission(RestPermissions.INDEXSETS_READ);

        final Set<String> indexWildcards = indexSetRegistry.getAll().stream()
                .map(IndexSet::getIndexWildcard)
                .collect(Collectors.toSet());
        final Set<IndexStatistics> indicesStats = indices.getIndicesStats(indexWildcards);
        final Set<String> closedIndices = indices.getClosedIndices(indexWildcards);
        return IndexSetStats.fromIndexStatistics(indicesStats, closedIndices);
    }

    @GET
    @Path("{id}")
    @Timed
    @ApiOperation(value = "Get index set")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Index set not found"),
    })
    public IndexSetSummary get(@ApiParam(name = "id", required = true)
                               @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_READ, id);
        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        return indexSetService.get(id)
                .map(config -> IndexSetSummary.fromIndexSetConfig(config, config.equals(defaultIndexSet)))
                .orElseThrow(() -> new NotFoundException("Couldn't load index set with ID <" + id + ">"));
    }

    @GET
    @Path("{id}/stats")
    @Timed
    @ApiOperation(value = "Get index set statistics")
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Index set not found"),
    })
    public IndexSetStats indexSetStatistics(@ApiParam(name = "id", required = true)
                                            @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_READ, id);
        return indexSetRegistry.get(id)
                .map(indexSetStatsCreator::getForIndexSet)
                .orElseThrow(() -> new NotFoundException("Couldn't load index set with ID <" + id + ">"));
    }

    @POST
    @Timed
    @ApiOperation(value = "Create index set")
    @RequiresPermissions(RestPermissions.INDEXSETS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.INDEX_SET_CREATE)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
    })
    public IndexSetSummary save(@ApiParam(name = "Index set configuration", required = true)
                                @Valid @NotNull IndexSetSummary indexSet) {
        try {
            final IndexSetConfig indexSetConfig = indexSet.toIndexSetConfig();

            final Optional<IndexSetValidator.Violation> violation = indexSetValidator.validate(indexSetConfig);
            if (violation.isPresent()) {
                throw new BadRequestException(violation.get().message());
            }

            final IndexSetConfig savedObject = indexSetService.save(indexSetConfig);
            final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
            return IndexSetSummary.fromIndexSetConfig(savedObject, savedObject.equals(defaultIndexSet));
        } catch (DuplicateKeyException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @PUT
    @Path("{id}")
    @Timed
    @ApiOperation(value = "Update index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_UPDATE)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
            @ApiResponse(code = 409, message = "Mismatch of IDs in URI path and payload"),
    })
    public IndexSetSummary update(@ApiParam(name = "id", required = true)
                                  @PathParam("id") String id,
                                  @ApiParam(name = "Index set configuration", required = true)
                                  @Valid @NotNull IndexSetUpdateRequest updateRequest) {
        checkPermission(RestPermissions.INDEXSETS_EDIT, id);

        final IndexSetConfig oldConfig = indexSetService.get(id)
                .orElseThrow(() -> new NotFoundException("Index set <" + id + "> not found"));

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        final boolean isDefaultSet = oldConfig.equals(defaultIndexSet);

        if (isDefaultSet && !updateRequest.isWritable()) {
            throw new ClientErrorException("Default index set must be writable.", Response.Status.CONFLICT);
        }

        final IndexSetConfig savedObject = indexSetService.save(updateRequest.toIndexSetConfig(id, oldConfig));

        return IndexSetSummary.fromIndexSetConfig(savedObject, isDefaultSet);
    }

    @PUT
    @Path("{id}/default")
    @Timed
    @ApiOperation(value = "Set default index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_UPDATE)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
    })
    public IndexSetSummary setDefault(@ApiParam(name = "id", required = true)
                                      @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_EDIT, id);

        final IndexSetConfig indexSet = indexSetService.get(id)
                .orElseThrow(() -> new NotFoundException("Index set <" + id + "> does not exist"));

        if (!indexSet.isWritable()) {
            throw new ClientErrorException("Default index set must be writable.", Response.Status.CONFLICT);
        }

        clusterConfigService.write(DefaultIndexSetConfig.create(indexSet.id()));

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();

        return IndexSetSummary.fromIndexSetConfig(indexSet, indexSet.equals(defaultIndexSet));
    }

    @DELETE
    @Path("{id}")
    @Timed
    @ApiOperation(value = "Delete index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_DELETE)
    @ApiResponses(value = {
            @ApiResponse(code = 403, message = "Unauthorized"),
            @ApiResponse(code = 404, message = "Index set not found"),
    })
    public void delete(@ApiParam(name = "id", required = true)
                       @PathParam("id") String id,
                       @ApiParam(name = "delete_indices")
                       @QueryParam("delete_indices") @DefaultValue("true") boolean deleteIndices) {
        checkPermission(RestPermissions.INDEXSETS_DELETE, id);

        final IndexSet indexSet = getIndexSet(indexSetRegistry, id);
        final IndexSet defaultIndexSet = indexSetRegistry.getDefault();

        if (indexSet.equals(defaultIndexSet)) {
            throw new BadRequestException("Default index set <" + indexSet.getConfig().id() + "> cannot be deleted!");
        }

        if (indexSetService.delete(id) == 0) {
            throw new NotFoundException("Couldn't delete index set with ID <" + id + ">");
        } else {
            if (deleteIndices) {
                try {
                    systemJobManager.submit(indexSetCleanupJobFactory.create(indexSet));
                } catch (SystemJobConcurrencyException e) {
                    LOG.error("Error running system job", e);
                }
            }
        }
    }
}
