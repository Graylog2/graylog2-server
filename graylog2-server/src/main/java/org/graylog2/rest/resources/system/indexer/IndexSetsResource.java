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
import com.mongodb.MongoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.IndexSetStatsCreator;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indexset.restrictions.IndexSetRestrictionsService;
import org.graylog2.indexer.indexset.validation.IndexSetValidator;
import org.graylog2.indexer.indexset.validation.IndexSetValidator.Violation;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.system.indices.DataTieringStatusService;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetsResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.LegacySystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@RequiresAuthentication
@Tag(name = "System/IndexSets", description = "Index sets")
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
    private final LegacySystemJobManager systemJobManager;
    private final DataTieringStatusService tieringStatusService;
    private final Set<OpenIndexSetFilterFactory> openIndexSetFilterFactories;
    private final IndexSetRestrictionsService indexSetRestrictionsService;

    @Inject
    public IndexSetsResource(final Indices indices,
                             final IndexSetService indexSetService,
                             final IndexSetRegistry indexSetRegistry,
                             final IndexSetValidator indexSetValidator,
                             final IndexSetCleanupJob.Factory indexSetCleanupJobFactory,
                             final IndexSetStatsCreator indexSetStatsCreator,
                             final ClusterConfigService clusterConfigService,
                             final LegacySystemJobManager systemJobManager,
                             final DataTieringStatusService tieringStatusService,
                             final Set<OpenIndexSetFilterFactory> openIndexSetFilterFactories, IndexSetRestrictionsService indexSetRestrictionsService) {
        this.indices = requireNonNull(indices);
        this.indexSetService = requireNonNull(indexSetService);
        this.indexSetRegistry = indexSetRegistry;
        this.indexSetValidator = indexSetValidator;
        this.indexSetCleanupJobFactory = requireNonNull(indexSetCleanupJobFactory);
        this.indexSetStatsCreator = indexSetStatsCreator;
        this.clusterConfigService = clusterConfigService;
        this.systemJobManager = systemJobManager;
        this.tieringStatusService = tieringStatusService;
        this.openIndexSetFilterFactories = openIndexSetFilterFactories;
        this.indexSetRestrictionsService = indexSetRestrictionsService;
    }

    @GET
    @Timed
    @Operation(summary = "Get a list of all index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns index sets", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetsResponse list(@Parameter(name = "skip", description = "The number of elements to skip (offset).", required = true)
                                  @QueryParam("skip") @DefaultValue("0") int skip,
                                  @Parameter(name = "limit", description = "The maximum number of elements to return.", required = true)
                                  @QueryParam("limit") @DefaultValue("0") int limit,
                                  @Parameter(name = "stats", description = "Include index set stats.")
                                  @QueryParam("stats") @DefaultValue("false") boolean computeStats,
                                  @Parameter(name = "only_open", description = "Include only graylog open indices.")
                                  @QueryParam("only_open") @DefaultValue("false") boolean onlyOpen) {

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        Stream<IndexSetConfig> indexSetConfigStream = indexSetService.findAll()
                .stream()
                .filter(indexSet -> isPermitted(RestPermissions.INDEXSETS_READ, indexSet.id()));
        if (onlyOpen) {
            for (OpenIndexSetFilterFactory filterFactory : openIndexSetFilterFactories) {
                indexSetConfigStream = indexSetConfigStream.filter(filterFactory.create());
            }
        }
        return getPagedIndexSetResponse(skip, limit, computeStats, defaultIndexSet, indexSetConfigStream.toList());
    }

    @GET
    @Path("search")
    @Timed
    @Operation(summary = "Get a list of all index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns index sets", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetsResponse search(@Parameter(name = "searchTitle", description = "The number of elements to skip (offset).")
                                    @QueryParam("searchTitle") String searchTitle,
                                    @Parameter(name = "skip", description = "The number of elements to skip (offset).", required = true)
                                    @QueryParam("skip") @DefaultValue("0") int skip,
                                    @Parameter(name = "limit", description = "The maximum number of elements to return.", required = true)
                                    @QueryParam("limit") @DefaultValue("0") int limit,
                                    @Parameter(name = "stats", description = "Include index set stats.")
                                    @QueryParam("stats") @DefaultValue("false") boolean computeStats) {
        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        List<IndexSetConfig> allowedConfigurations = indexSetService.searchByTitle(searchTitle).stream()
                .filter(indexSet -> isPermitted(RestPermissions.INDEXSETS_READ, indexSet.id())).toList();

        return getPagedIndexSetResponse(skip, limit, computeStats, defaultIndexSet, allowedConfigurations);
    }

    private IndexSetsResponse getPagedIndexSetResponse(int skip, int limit, boolean computeStats, IndexSetConfig defaultIndexSet, List<IndexSetConfig> allowedConfigurations) {
        int calculatedLimit = limit > 0 ? limit : allowedConfigurations.size();
        Comparator<IndexSetConfig> titleComparator = Comparator.comparing(IndexSetConfig::title, String.CASE_INSENSITIVE_ORDER);

        List<IndexSetConfig> pagedConfigs = allowedConfigurations.stream()
                .sorted(titleComparator)
                .skip(skip)
                .limit(calculatedLimit)
                .toList();

        List<IndexSetResponse> indexSets = pagedConfigs.stream()
                .map(config -> IndexSetResponse.fromIndexSetConfig(config, config.equals(defaultIndexSet), null))
                .toList();


        Map<String, IndexSetStats> stats = Collections.emptyMap();

        if (computeStats) {
            stats = indexSetRegistry.getFromIndexConfig(pagedConfigs).stream()
                    .collect(Collectors.toMap(indexSet -> indexSet.getConfig().id(), indexSetStatsCreator::getForIndexSet));
        }

        return IndexSetsResponse.create(allowedConfigurations.size(), indexSets, stats);
    }


    @GET
    @Path("stats")
    @Timed
    @Operation(summary = "Get stats of all index sets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns global stats", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetStats globalStats() {
        checkPermission(RestPermissions.INDEXSETS_READ);
        return indices.getIndexSetStats();
    }

    @GET
    @Path("{id}")
    @Timed
    @Operation(summary = "Get index set")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the index set", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Index set not found"),
    })
    public IndexSetResponse get(@Parameter(name = "id", required = true)
                                @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_READ, id);
        final IndexSet indexSet = indexSetRegistry.get(id).orElseThrow(() -> new NotFoundException("Couldn't find index set with ID <" + id + ">"));
        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        return indexSetService.get(id)
                .map(config -> IndexSetResponse.fromIndexSetConfig(
                        config,
                        config.equals(defaultIndexSet),
                        tieringStatusService.getStatus(indexSet, config)))
                .orElseThrow(() -> new NotFoundException("Couldn't load index set with ID <" + id + ">"));
    }

    @GET
    @Path("{id}/stats")
    @Timed
    @Operation(summary = "Get index set statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns statistics", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Index set not found"),
    })
    public IndexSetStats indexSetStatistics(@Parameter(name = "id", required = true)
                                            @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_READ, id);
        return indexSetRegistry.get(id)
                .map(indexSetStatsCreator::getForIndexSet)
                .orElseThrow(() -> new NotFoundException("Couldn't load index set with ID <" + id + ">"));
    }

    @POST
    @Timed
    @Operation(summary = "Create index set")
    @RequiresPermissions(RestPermissions.INDEXSETS_CREATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.INDEX_SET_CREATE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns created index set", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetResponse save(@Parameter(name = "Index set configuration", required = true)
                                 @Valid @NotNull IndexSetCreationRequest indexSet) {
        try {
            checkDataTieringNotNull(indexSet.useLegacyRotation(), indexSet.dataTieringConfig());
            final IndexSetConfig indexSetConfig = indexSetRestrictionsService.createIndexSetConfig(indexSet, isPermitted(RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT));

            final Optional<Violation> violation = indexSetValidator.validate(indexSetConfig);
            if (violation.isPresent()) {
                throw new BadRequestException(violation.get().message());
            }

            final IndexSetConfig savedObject = indexSetService.save(indexSetConfig);
            final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
            return IndexSetResponse.fromIndexSetConfig(savedObject, savedObject.equals(defaultIndexSet), null);
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                throw new BadRequestException(e.getMessage());
            }
            throw e;
        }
    }

    @PUT
    @Path("{id}")
    @Timed
    @Operation(summary = "Update index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_UPDATE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns updated index set", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "409", description = "Mismatch of IDs in URI path and payload"),
    })
    public IndexSetResponse update(@Parameter(name = "id", required = true)
                                   @PathParam("id") String id,
                                   @Parameter(name = "Index set configuration", required = true)
                                   @Valid @NotNull IndexSetUpdateRequest updateRequest) {
        checkPermission(RestPermissions.INDEXSETS_EDIT, id);

        final IndexSetConfig oldConfig = indexSetService.get(id)
                .orElseThrow(() -> new NotFoundException("Index set <" + id + "> not found"));

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        final boolean isDefaultSet = oldConfig.equals(defaultIndexSet);

        if (isDefaultSet && !updateRequest.isWritable()) {
            throw new ClientErrorException("Default index set must be writable.", Response.Status.CONFLICT);
        }

        checkDataTieringNotNull(updateRequest.useLegacyRotation(), updateRequest.dataTieringConfig());

        final IndexSetConfig indexSetConfig = indexSetRestrictionsService.updateIndexSetConfig(updateRequest, oldConfig,
                isPermitted(RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT));

        final Optional<Violation> violation = indexSetValidator.validate(indexSetConfig);
        if (violation.isPresent()) {
            throw new BadRequestException(violation.get().message());
        }

        final IndexSetConfig savedObject = indexSetService.save(indexSetConfig);

        return IndexSetResponse.fromIndexSetConfig(savedObject, isDefaultSet, null);
    }

    private void checkDataTieringNotNull(Boolean useLegacyRotation, DataTieringConfig dataTieringConfig) {
        Violation violation = indexSetValidator.checkDataTieringNotNull(useLegacyRotation, dataTieringConfig);
        if (violation != null) {
            throw new BadRequestException(violation.message());
        }
    }

    @PUT
    @Path("{id}/default")
    @Timed
    @Operation(summary = "Set default index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_UPDATE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns default index set", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetResponse setDefault(@Parameter(name = "id", required = true)
                                       @PathParam("id") String id) {
        checkPermission(RestPermissions.INDEXSETS_EDIT, id);

        final IndexSetConfig indexSet = indexSetService.get(id)
                .orElseThrow(() -> new NotFoundException("Index set <" + id + "> does not exist"));

        if (!indexSet.isRegularIndex()) {
            throw new ClientErrorException("Index set not eligible as default", Response.Status.CONFLICT);
        }

        clusterConfigService.write(DefaultIndexSetConfig.create(indexSet.id()));

        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();

        return IndexSetResponse.fromIndexSetConfig(indexSet, indexSet.equals(defaultIndexSet), null);
    }

    @DELETE
    @Path("{id}")
    @Timed
    @Operation(summary = "Delete index set")
    @AuditEvent(type = AuditEventTypes.INDEX_SET_DELETE)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Success"),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Index set not found"),
    })
    public void delete(@Parameter(name = "id", required = true)
                       @PathParam("id") String id,
                       @Parameter(name = "delete_indices")
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
