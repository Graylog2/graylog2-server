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
import com.google.common.eventbus.EventBus;
import com.mongodb.MongoException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.apache.shiro.subject.Subject;
import org.bson.conversions.Bson;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.cluster.lock.AlreadyLockedException;
import org.graylog2.cluster.lock.RefreshingLockService;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.datatiering.DataTieringConfig;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.indexer.indexset.IndexSet;
import org.graylog2.indexer.indexset.IndexSetAttributeSorts;
import org.graylog2.indexer.indexset.IndexSetCategory;
import org.graylog2.indexer.indexset.IndexSetCategoryCounts;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.IndexSetStatsCreator;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.indexset.PaginatedIndexSetService;
import org.graylog2.indexer.indexset.RotationModel;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfile;
import org.graylog2.indexer.indexset.profile.IndexFieldTypeProfileService;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.indexset.restrictions.IndexSetRestrictionsService;
import org.graylog2.indexer.indexset.validation.IndexSetValidator;
import org.graylog2.indexer.indexset.validation.IndexSetValidator.Violation;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.events.IndicesDeletedEvent;
import org.graylog2.indexer.indices.jobs.IndexSetCleanupJob;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.system.indices.DataTieringStatusService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetCreationRequest;
import org.graylog2.rest.resources.system.indexer.requests.IndexSetUpdateRequest;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetOverviewResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetResponse;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetStats;
import org.graylog2.rest.resources.system.indexer.responses.IndexSetsResponse;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.EntityPermissionsUtils;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.system.jobs.LegacySystemJobManager;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

@RequiresAuthentication
@Tag(name = "System/IndexSets", description = "Index sets")
@Path("/system/indices/index_sets")
@Produces(MediaType.APPLICATION_JSON)
public class IndexSetsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(IndexSetsResource.class);
    private static final String DEFAULT_SORT_FIELD = IndexSetConfig.FIELD_TITLE;
    private static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(IndexSetConfig.FIELD_TITLE).title("Title").searchable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_DESCRIPTION).title("Description").sortable(false).searchable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_INDEX_PREFIX).title("Index prefix").filterable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_SHARDS).title("Shards").type(SearchQueryField.Type.INT).filterable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_REPLICAS).title("Replicas").type(SearchQueryField.Type.INT).filterable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_CREATION_DATE).title("Created").type(SearchQueryField.Type.DATE).filterable(true).build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_INDEX_TEMPLATE_TYPE).title("Template type").filterable(true).build(),
            EntityAttribute.builder().id("category").title("Category").sortable(false).filterable(true)
                    .filterOptions(Arrays.stream(IndexSetCategory.values()).map(IndexSetCategory::filterOption).collect(toSet()))
                    .bsonFilterCreator((field, value) -> IndexSetCategory.fromValue(value.getValue().toString()).toBson())
                    .build(),
            EntityAttribute.builder().id(IndexSetConfig.FIELD_PROFILE_ID).title("Field type profile").filterable(true)
                    .relatedCollection(IndexFieldTypeProfileService.INDEX_FIELD_TYPE_PROFILE_MONGO_COLLECTION_NAME)
                    .relatedProperty(IndexFieldTypeProfile.NAME_FIELD_NAME)
                    .sortSpec(IndexSetAttributeSorts.profileTitle())
                    .build(),
            EntityAttribute.builder().id("stream_count").title("Associated streams").type(SearchQueryField.Type.INT)
                    .sortSpec(IndexSetAttributeSorts.streamCount())
                    .build(),
            EntityAttribute.builder().id("rotation_model").title("Rotation model").sortable(false).filterable(true)
                    .filterOptions(Arrays.stream(RotationModel.values()).map(RotationModel::filterOption).collect(toSet()))
                    .bsonFilterCreator((field, value) -> RotationModel.fromValue(value.getValue().toString()).toBson())
                    .build()
    );
    private static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.ASC))
            .build();

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
    private final EventBus eventBus;
    private final RefreshingLockService.Factory lockServiceFactory;
    private final PaginatedIndexSetService paginatedIndexSetService;
    private final StreamService streamService;
    private final EntityPermissionsUtils entityPermissionsUtils;
    private final DbQueryCreator dbQueryCreator = new DbQueryCreator(DEFAULT_SORT_FIELD, ATTRIBUTES);

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
                             final Set<OpenIndexSetFilterFactory> openIndexSetFilterFactories, IndexSetRestrictionsService indexSetRestrictionsService,
                             final EventBus eventBus,
                             final RefreshingLockService.Factory lockServiceFactory,
                             final PaginatedIndexSetService paginatedIndexSetService,
                             final StreamService streamService,
                             final EntityPermissionsUtils entityPermissionsUtils) {
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
        this.eventBus = eventBus;
        this.lockServiceFactory = lockServiceFactory;
        this.paginatedIndexSetService = requireNonNull(paginatedIndexSetService);
        this.streamService = requireNonNull(streamService);
        this.entityPermissionsUtils = requireNonNull(entityPermissionsUtils);
    }

    @GET
    @Path("paginated")
    @Timed
    @Operation(summary = "Get a paginated list of index sets, with sorting and filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns one page of index sets", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public PageListResponse<IndexSetOverviewResponse> getPage(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                      @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
                                                      @Parameter(name = "sort", description = "The field to sort the result on",
                                                                 schema = @Schema(allowableValues = {"title", "index_prefix", "shards", "replicas", "creation_date", "index_template_type", "field_type_profile", "stream_count"}))
                                                      @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                      @Parameter(name = "order", description = "The sort direction",
                                                                 schema = @Schema(allowableValues = {"asc", "desc"}))
                                                      @DefaultValue("asc") @QueryParam("order") SortOrder order) {
        final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
        final DbSortResolver.ResolvedSort resolvedSort = DbSortResolver.resolve(ATTRIBUTES, sort, order);
        // A user who may read every index set pages in MongoDB. Anyone else needs the per-entity predicate;
        // the helper applies it after fetching, so totals stay consistent.
        final PaginatedList<IndexSetConfig> result = canReadAllIndexSets()
                ? paginatedIndexSetService.findPaginated(dbQuery, resolvedSort, page, perPage)
                : paginatedIndexSetService.findPaginated(dbQuery, this::mayRead, resolvedSort, page, perPage);
        final IndexSetConfig defaultIndexSet = indexSetService.getDefault();
        final Map<String, Long> streamCounts = streamService.countByIndexSet(result.stream().map(IndexSetConfig::id).toList());
        final List<IndexSetOverviewResponse> elements = result.stream()
                .map(config -> IndexSetOverviewResponse.create(config, config.equals(defaultIndexSet), streamCounts.getOrDefault(config.id(), 0L)))
                .toList();

        return PageListResponse.create(query, result.pagination(), result.pagination().total(), sort, order, elements, ATTRIBUTES, DEFAULTS);
    }

    @GET
    @Path("categories/count")
    @Timed
    @Operation(summary = "Count the index sets the caller may read, overall and per category")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns the counts", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetCategoryCounts getCategoryCounts() {
        return canReadAllIndexSets()
                ? paginatedIndexSetService.countCategories()
                : paginatedIndexSetService.countCategories(this::mayRead);
    }

    private boolean canReadAllIndexSets() {
        final Subject subject = getSubject();
        return entityPermissionsUtils.hasAllPermission(subject)
                || entityPermissionsUtils.hasReadPermissionForWholeCollection(subject, MongoIndexSetService.COLLECTION_NAME);
    }

    private boolean mayRead(IndexSetConfig config) {
        return isPermitted(RestPermissions.INDEXSETS_READ, config.id());
    }

    /**
     * @deprecated Use {@link #getPage} ({@code GET /system/indices/index_sets/paginated}); it sorts, filters, and paginates in the database.
     */
    @Deprecated(forRemoval = true)
    @GET
    @Timed
    @Operation(summary = "Get a list of all index sets", deprecated = true)
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

    /**
     * @deprecated Use {@link #getPage} ({@code GET /system/indices/index_sets/paginated}) with the {@code query} parameter.
     */
    @Deprecated(forRemoval = true)
    @GET
    @Path("search")
    @Timed
    @Operation(summary = "Search index sets by title", deprecated = true)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Returns index sets", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "403", description = "Unauthorized"),
    })
    public IndexSetsResponse search(@Parameter(name = "searchTitle", description = "The number of elements to skip (offset).")
                                    @QueryParam("searchTitle") @DefaultValue("") String searchTitle,
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
            @ApiResponse(responseCode = "409", description = "A concurrent operation holds the selected repository's lock"),
    })
    public IndexSetResponse save(@Parameter(name = "Index set configuration", required = true)
                                 @Valid @NotNull IndexSetCreationRequest indexSet) {
        try {
            checkDataTieringNotNull(indexSet.useLegacyRotation(), indexSet.dataTieringConfig());
            final IndexSetConfig indexSetConfig = indexSetRestrictionsService.createIndexSetConfig(indexSet, isPermitted(RestPermissions.INDEXSETS_FIELD_RESTRICTIONS_EDIT));

            final IndexSetConfig savedObject = validateAndSaveWithRepositoryLock(indexSetConfig);
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
            @ApiResponse(responseCode = "409", description = "The default index set would become non-writable, or a concurrent operation holds the selected repository's lock"),
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

        final IndexSetConfig savedObject = validateAndSaveWithRepositoryLock(indexSetConfig);

        return IndexSetResponse.fromIndexSetConfig(savedObject, isDefaultSet, null);
    }

    // Keeps a concurrent repository delete from missing the reference this save adds. Covers operator
    // edits only, internally created index sets inherit their repository from the default template,
    // which is itself a reference that blocks the delete.
    private IndexSetConfig validateAndSaveWithRepositoryLock(IndexSetConfig indexSetConfig) {
        final Optional<String> repositoryLockId = Optional.ofNullable(indexSetConfig.dataTieringConfig())
                .flatMap(DataTieringConfig::repositoryLockId);
        if (repositoryLockId.isEmpty()) {
            return validateAndSave(indexSetConfig);
        }
        try (RefreshingLockService lockService = lockServiceFactory.create()) {
            lockService.acquireAndKeepLock(repositoryLockId.get(), 1);
            return validateAndSave(indexSetConfig);
        } catch (AlreadyLockedException e) {
            throw new ClientErrorException(e.getMessage(), Response.Status.CONFLICT);
        }
    }

    private IndexSetConfig validateAndSave(IndexSetConfig indexSetConfig) {
        final Optional<Violation> violation = indexSetValidator.validate(indexSetConfig);
        if (violation.isPresent()) {
            throw new BadRequestException(violation.get().message());
        }
        return indexSetService.save(indexSetConfig);
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
            } else {
                final String[] managedIndices = indexSet.getManagedIndices();
                if (managedIndices != null) {
                    for (String indexName : managedIndices) {
                        eventBus.post(IndicesDeletedEvent.create(indexName));
                    }
                }
            }
        }
    }
}
