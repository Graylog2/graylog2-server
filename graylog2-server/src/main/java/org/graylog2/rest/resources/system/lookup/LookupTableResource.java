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
package org.graylog2.rest.resources.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
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
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.lookup.CachePurge;
import org.graylog2.lookup.LookupDefaultMultiValue;
import org.graylog2.lookup.LookupDefaultSingleValue;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.lookup.adapters.LookupDataAdapterValidationContext;
import org.graylog2.lookup.db.DBCacheService;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.db.DBLookupTableService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.rest.resources.system.responses.LookupTableCachePurgingResponse;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.plugin.lookup.LookupResult;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.rest.models.system.lookup.CacheApi;
import org.graylog2.rest.models.system.lookup.DataAdapterApi;
import org.graylog2.rest.models.system.lookup.ErrorStates;
import org.graylog2.rest.models.system.lookup.ErrorStatesRequest;
import org.graylog2.rest.models.system.lookup.LookupTableApi;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

@RequiresAuthentication
@Path("/system/lookup")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup", description = "Lookup tables")
public class LookupTableResource extends RestResource {
    private static final ImmutableSet<String> LUT_ALLOWABLE_SORT_FIELDS = ImmutableSet.of(
            LookupTableDto.FIELD_ID,
            LookupTableDto.FIELD_TITLE,
            LookupTableDto.FIELD_DESCRIPTION,
            LookupTableDto.FIELD_NAME
    );
    private static final ImmutableSet<String> ADAPTER_ALLOWABLE_SORT_FIELDS = ImmutableSet.of(
            DataAdapterDto.FIELD_ID,
            DataAdapterDto.FIELD_TITLE,
            DataAdapterDto.FIELD_DESCRIPTION,
            DataAdapterDto.FIELD_NAME
    );
    private static final ImmutableSet<String> CACHE_ALLOWABLE_SORT_FIELDS = ImmutableSet.of(
            CacheDto.FIELD_ID,
            CacheDto.FIELD_TITLE,
            CacheDto.FIELD_DESCRIPTION,
            CacheDto.FIELD_NAME
    );
    private static final ImmutableMap<String, SearchQueryField> LUT_SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(LookupTableDto.FIELD_ID))
            .put("title", SearchQueryField.create(LookupTableDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(LookupTableDto.FIELD_DESCRIPTION))
            .put("name", SearchQueryField.create(LookupTableDto.FIELD_NAME))
            .build();
    private static final ImmutableMap<String, SearchQueryField> ADAPTER_SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(DataAdapterDto.FIELD_ID))
            .put("title", SearchQueryField.create(DataAdapterDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(DataAdapterDto.FIELD_DESCRIPTION))
            .put("name", SearchQueryField.create(DataAdapterDto.FIELD_NAME))
            .build();
    private static final ImmutableMap<String, SearchQueryField> CACHE_SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(CacheDto.FIELD_ID))
            .put("title", SearchQueryField.create(CacheDto.FIELD_TITLE))
            .put("description", SearchQueryField.create(CacheDto.FIELD_DESCRIPTION))
            .put("name", SearchQueryField.create(CacheDto.FIELD_NAME))
            .build();

    private final DBLookupTableService dbTableService;
    private final DBDataAdapterService dbDataAdapterService;
    private final DBCacheService dbCacheService;
    private final Map<String, LookupCache.Factory> cacheTypes;
    private final Map<String, LookupDataAdapter.Factory> dataAdapterTypes;
    private final Map<String, LookupDataAdapter.Factory2> dataAdapterTypes2;
    private final SearchQueryParser lutSearchQueryParser;
    private final SearchQueryParser adapterSearchQueryParser;
    private final SearchQueryParser cacheSearchQueryParser;
    private final LookupTableService lookupTableService;
    private final LookupDataAdapterValidationContext lookupDataAdapterValidationContext;

    @Inject
    public LookupTableResource(DBLookupTableService dbTableService, DBDataAdapterService dbDataAdapterService,
                               DBCacheService dbCacheService, Map<String, LookupCache.Factory> cacheTypes,
                               Map<String, LookupDataAdapter.Factory> dataAdapterTypes,
                               Map<String, LookupDataAdapter.Factory2> dataAdapterTypes2,
                               LookupTableService lookupTableService,
                               LookupDataAdapterValidationContext lookupDataAdapterValidationContext) {
        this.dbTableService = dbTableService;
        this.dbDataAdapterService = dbDataAdapterService;
        this.dbCacheService = dbCacheService;
        this.cacheTypes = cacheTypes;
        this.dataAdapterTypes = dataAdapterTypes;
        this.dataAdapterTypes2 = dataAdapterTypes2;
        this.lookupTableService = lookupTableService;
        this.lookupDataAdapterValidationContext = lookupDataAdapterValidationContext;
        this.lutSearchQueryParser = new SearchQueryParser(LookupTableDto.FIELD_TITLE, LUT_SEARCH_FIELD_MAPPING);
        this.adapterSearchQueryParser = new SearchQueryParser(DataAdapterDto.FIELD_TITLE, ADAPTER_SEARCH_FIELD_MAPPING);
        this.cacheSearchQueryParser = new SearchQueryParser(CacheDto.FIELD_TITLE, CACHE_SEARCH_FIELD_MAPPING);
    }

    private void checkLookupTableId(String idOrName, LookupTableApi toUpdate) {
        requireNonNull(idOrName, "idOrName parameter cannot be null");
        if (idOrName.equals(toUpdate.id()) || idOrName.equals(toUpdate.name())) {
            return;
        }
        throw new BadRequestException("URL parameter <" + idOrName + "> does not match parameter in request body");
    }
    private void checkLookupCacheId(String idOrName, CacheApi toUpdate) {
        requireNonNull(idOrName, "idOrName parameter cannot be null");
        if (idOrName.equals(toUpdate.id()) || idOrName.equals(toUpdate.name())) {
            return;
        }
        throw new BadRequestException("URL parameter <" + idOrName + "> does not match parameter in request body");
    }
    private void checkLookupAdapterId(String idOrName, DataAdapterApi toUpdate) {
        requireNonNull(idOrName, "idOrName parameter cannot be null");
        if (idOrName.equals(toUpdate.id()) || idOrName.equals(toUpdate.name())) {
            return;
        }
        throw new BadRequestException("URL parameter <" + idOrName + "> does not match parameter in request body");
    }

    @GET
    @Path("tables/{name}/query")
    @ApiOperation(value = "Query a lookup table")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public LookupResult performLookup(@ApiParam(name = "name") @PathParam("name") @NotEmpty String name,
                                      @ApiParam(name = "key") @QueryParam("key") @NotEmpty String key) {
        return lookupTableService.newBuilder().lookupTable(name).build().lookup(key);
    }

    /**
     * NOTE: Must NOT be called directly by clients. Consider calling
     * {@link org.graylog2.rest.resources.cluster.ClusterLookupTableResource#performPurge(String, String)}
     * instead!
     */
    @POST
    @Path("tables/{idOrName}/purge")
    @ApiOperation(value = "Purge lookup table cache")
    @NoAuditEvent("Cache purge only")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public LookupTableCachePurgingResponse performPurge(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                                                        @ApiParam(name = "key") @QueryParam("key") String key) {
        final Optional<LookupTableDto> lookupTableDto = dbTableService.get(idOrName);
        if (!lookupTableDto.isPresent()) {
            throw new NotFoundException("Lookup table <" + idOrName + "> not found");
        }

        final Optional<CachePurge> cachePurge = lookupTableService.newCachePurge(lookupTableDto.get().name());
        if (cachePurge.isPresent()) {
            if (isNullOrEmpty(key)) {
                cachePurge.get().purgeAll();
            } else {
                cachePurge.get().purgeKey(key);
            }
        } else {
            throw new NotFoundException("Lookup table <" + idOrName + "> not found");
        }

        return LookupTableCachePurgingResponse.success();
    }

    @GET
    @Path("tables")
    @ApiOperation(value = "List configured lookup tables")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public LookupTablePage tables(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                  @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                  @ApiParam(name = "sort",
                                          value = "The field to sort the result on",
                                          required = true,
                                          allowableValues = "title,description,name,id")
                                  @DefaultValue(LookupTableDto.FIELD_TITLE) @QueryParam("sort") String sort,
                                  @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                  @DefaultValue("desc") @QueryParam("order") String order,
                                  @ApiParam(name = "query") @QueryParam("query") String query,
                                  @ApiParam(name = "resolve") @QueryParam("resolve") @DefaultValue("false") boolean resolveObjects) {

        if (!LUT_ALLOWABLE_SORT_FIELDS.contains(sort.toLowerCase(Locale.ENGLISH))) {
            sort = LookupTableDto.FIELD_TITLE;
        }
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(sort);
        } else {
            sortBuilder = DBSort.asc(sort);
        }

        try {
            final SearchQuery searchQuery = lutSearchQueryParser.parse(query);
            final DBQuery.Query dbQuery = searchQuery.toDBQuery();

            PaginatedList<LookupTableDto> paginated = dbTableService.findPaginated(dbQuery, sortBuilder, page, perPage);

            ImmutableSet.Builder<CacheApi> caches = ImmutableSet.builder();
            ImmutableSet.Builder<DataAdapterApi> dataAdapters = ImmutableSet.builder();
            if (resolveObjects) {
                ImmutableSet.Builder<String> cacheIds = ImmutableSet.builder();
                ImmutableSet.Builder<String> dataAdapterIds = ImmutableSet.builder();

                paginated.forEach(dto -> {
                    cacheIds.add(dto.cacheId());
                    dataAdapterIds.add(dto.dataAdapterId());
                });

                dbCacheService.findByIds(cacheIds.build()).forEach(cacheDto -> caches.add(CacheApi.fromDto(cacheDto)));
                dbDataAdapterService.findByIds(dataAdapterIds.build()).forEach(dataAdapterDto -> dataAdapters.add(DataAdapterApi.fromDto(dataAdapterDto)));
            }

            return new LookupTablePage(query,
                    paginated.pagination(),
                    paginated.stream().map(LookupTableApi::fromDto).collect(Collectors.toList()),
                    caches.build(),
                    dataAdapters.build());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @GET
    @Path("tables/{idOrName}")
    @ApiOperation(value = "Retrieve the named lookup table")
    public LookupTablePage get(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                               @ApiParam(name = "resolve") @QueryParam("resolve") @DefaultValue("false") boolean resolveObjects) {

        Optional<LookupTableDto> lookupTableDto = dbTableService.get(idOrName);
        if (!lookupTableDto.isPresent()) {
            throw new NotFoundException();
        }
        LookupTableDto tableDto = lookupTableDto.get();

        checkPermission(RestPermissions.LOOKUP_TABLES_READ, tableDto.id());

        Set<CacheApi> caches = Collections.emptySet();
        Set<DataAdapterApi> adapters = Collections.emptySet();

        if (resolveObjects) {
            caches = dbCacheService.findByIds(Collections.singleton(tableDto.cacheId())).stream().map(CacheApi::fromDto).collect(Collectors.toSet());
            adapters = dbDataAdapterService.findByIds(Collections.singleton(tableDto.dataAdapterId())).stream().map(DataAdapterApi::fromDto).collect(Collectors.toSet());
        }

        final PaginatedList<LookupTableApi> result = PaginatedList.singleton(LookupTableApi.fromDto(tableDto), 1, 1);
        return new LookupTablePage(null,
                result.pagination(),
                result,
                caches,
                adapters);
    }


    @POST
    @Path("tables")
    @AuditEvent(type = AuditEventTypes.LOOKUP_TABLE_CREATE)
    @ApiOperation(value = "Create a new lookup table")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_CREATE)
    public LookupTableApi createTable(@ApiParam LookupTableApi lookupTable) {
        try {
            LookupTableDto saved = dbTableService.save(lookupTable.toDto());

            return LookupTableApi.fromDto(saved);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @PUT
    @Path("tables/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_TABLE_UPDATE)
    @ApiOperation(value = "Update the given lookup table")
    public LookupTableApi updateTable(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                                      @Valid @ApiParam LookupTableApi toUpdate) {
        checkLookupTableId(idOrName, toUpdate);
        checkPermission(RestPermissions.LOOKUP_TABLES_EDIT, toUpdate.id());
        LookupTableDto saved = dbTableService.save(toUpdate.toDto());

        return LookupTableApi.fromDto(saved);
    }

    @DELETE
    @Path("tables/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_TABLE_DELETE)
    @ApiOperation(value = "Delete the lookup table")
    public LookupTableApi removeTable(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that table isn't in use, how?
        Optional<LookupTableDto> lookupTableDto = dbTableService.get(idOrName);
        if (!lookupTableDto.isPresent()) {
            throw new NotFoundException();
        }
        checkPermission(RestPermissions.LOOKUP_TABLES_DELETE, lookupTableDto.get().id());
        dbTableService.delete(idOrName);

        return LookupTableApi.fromDto(lookupTableDto.get());
    }

    @POST
    @Path("tables/validate")
    @NoAuditEvent("Validation only")
    @ApiOperation(value = "Validate the lookup table config")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public ValidationResult validateTable(@Valid @ApiParam LookupTableApi toValidate) {
        final ValidationResult validation = new ValidationResult();

        final Optional<LookupTableDto> dtoOptional = dbTableService.get(toValidate.name());
        if (dtoOptional.isPresent()) {
            // a table exist with the given name, check that the IDs are the same, this might be an update
            final LookupTableDto tableDto = dtoOptional.get();
            //noinspection ConstantConditions
            if (!tableDto.id().equals(toValidate.id())) {
                // a table exists with a different id, so the name is already in use, fail validation
                validation.addError("name", "The lookup table name is already in use.");
            }
        }

        try {
            LookupDefaultSingleValue.create(toValidate.defaultSingleValue(), toValidate.defaultSingleValueType());
        } catch (Exception e) {
            validation.addError(LookupTableApi.FIELD_DEFAULT_SINGLE_VALUE, e.getMessage());
        }
        try {
            LookupDefaultMultiValue.create(toValidate.defaultMultiValue(), toValidate.defaultMultiValueType());
        } catch (Exception e) {
            validation.addError(LookupTableApi.FIELD_DEFAULT_MULTI_VALUE, e.getMessage());
        }

        return validation;
    }

    @JsonAutoDetect
    public static class LookupTablePage {

        @Nullable
        @JsonProperty
        private final String query;

        @JsonUnwrapped
        private final PaginatedList.PaginationInfo paginationInfo;

        @JsonProperty("lookup_tables")
        private final List<LookupTableApi> lookupTables;

        @JsonProperty("caches")
        private final Map<String, CacheApi> cacheApiMap;

        @JsonProperty("data_adapters")
        private final Map<String, DataAdapterApi> dataAdapterMap;

        public LookupTablePage(@Nullable String query,
                               PaginatedList.PaginationInfo paginationInfo,
                               List<LookupTableApi> lookupTables,
                               Collection<CacheApi> caches,
                               Collection<DataAdapterApi> dataAdapters) {
            this.query = query;
            this.paginationInfo = paginationInfo;
            this.lookupTables = lookupTables;
            this.cacheApiMap = Maps.uniqueIndex(caches, CacheApi::id);
            this.dataAdapterMap = Maps.uniqueIndex(dataAdapters, DataAdapterApi::id);
        }
    }

    @GET
    @Path("adapters")
    @ApiOperation(value = "List available data adapters")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public DataAdapterPage adapters(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                @ApiParam(name = "sort",
                                        value = "The field to sort the result on",
                                        required = true,
                                        allowableValues = "title,description,name,id")
                                @DefaultValue(DataAdapterDto.FIELD_TITLE) @QueryParam("sort") String sort,
                                @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                @DefaultValue("desc") @QueryParam("order") String order,
                                @ApiParam(name = "query") @QueryParam("query") String query) {

        if (!ADAPTER_ALLOWABLE_SORT_FIELDS.contains(sort.toLowerCase(Locale.ENGLISH))) {
            sort = DataAdapterDto.FIELD_TITLE;
        }
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(sort);
        } else {
            sortBuilder = DBSort.asc(sort);
        }

        try {
            final SearchQuery searchQuery = adapterSearchQueryParser.parse(query);
            final DBQuery.Query dbQuery = searchQuery.toDBQuery();

            PaginatedList<DataAdapterDto> paginated = dbDataAdapterService.findPaginated(dbQuery, sortBuilder, page, perPage);
            return new DataAdapterPage(query,
                    paginated.pagination(),
                    paginated.stream().map(DataAdapterApi::fromDto).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @GET
    @Path("types/adapters")
    @ApiOperation(value = "List available data adapter types")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public Map<String, LookupDataAdapter.Descriptor> availableAdapterTypes() {

        final Stream<LookupDataAdapter.Descriptor> stream1 = dataAdapterTypes.values().stream().map(LookupDataAdapter.Factory::getDescriptor);
        final Stream<LookupDataAdapter.Descriptor> stream2 = dataAdapterTypes2.values().stream().map(LookupDataAdapter.Factory2::getDescriptor);
        return Stream.concat(stream1, stream2)
                .collect(Collectors.toMap(LookupDataAdapter.Descriptor::getType, Function.identity()));

    }

    @POST
    @NoAuditEvent("Bulk read call")
    @Path("errorstates")
    @ApiOperation(value = "Retrieve the runtime error states of the given lookup tables, caches and adapters")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public ErrorStates errorStates(@ApiParam(name = "request") @Valid ErrorStatesRequest request) {
        final ErrorStates.Builder errorStates = ErrorStates.builder();
        if (request.tables() != null) {
            //noinspection ConstantConditions
            for (String tableName : request.tables()) {

                final LookupTable table = lookupTableService.newBuilder().lookupTable(tableName).build().getTable();
                if (table != null) {
                    errorStates.tables().put(tableName, table.error());
                }
            }
        }
        if (request.dataAdapters() != null) {
            lookupTableService.getDataAdapters(request.dataAdapters()).forEach(adapter -> {
                errorStates.dataAdapters().put(adapter.name(), adapter.getError().map(Throwable::getMessage).orElse(null));
            });
        }
        if (request.caches() != null) {
            lookupTableService.getCaches(request.caches()).forEach(cache -> {
                errorStates.caches().put(cache.name(), cache.getError().map(Throwable::getMessage).orElse(null));
            });
        }
        return errorStates.build();
    }

    @GET
    @Path("adapters/{idOrName}")
    @ApiOperation(value = "List the given data adapter")
    public DataAdapterApi getAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<DataAdapterDto> dataAdapterDto = dbDataAdapterService.get(idOrName);
        if (dataAdapterDto.isPresent()) {
            checkPermission(RestPermissions.LOOKUP_TABLES_READ, dataAdapterDto.get().id());
            return DataAdapterApi.fromDto(dataAdapterDto.get());
        }
        throw new NotFoundException();
    }

    @GET
    @Path("adapters/{name}/query")
    @ApiOperation(value = "Query a lookup table")
    @ApiResponses({
            @ApiResponse(code = 404, message = "If the adapter cannot be found (if it failed or doesn't exist at all)")
    })
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public LookupResult performAdapterLookup(@ApiParam(name = "name") @PathParam("name") @NotEmpty String name,
                                             @ApiParam(name = "key") @QueryParam("key") @NotEmpty String key) {
        final Collection<LookupDataAdapter> dataAdapters = lookupTableService.getDataAdapters(singleton(name));
        if (!dataAdapters.isEmpty()) {
            return Iterables.getOnlyElement(dataAdapters).get(key);
        } else {
            throw new NotFoundException("Unable to find data adapter " + name);
        }
    }

    @POST
    @Path("adapters")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_CREATE)
    @ApiOperation(value = "Create a new data adapter")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_CREATE)
    public DataAdapterApi createAdapter(@Valid @ApiParam DataAdapterApi newAdapter) {
        try {
            DataAdapterDto dto = newAdapter.toDto();
            DataAdapterDto saved = dbDataAdapterService.save(dto);

            return DataAdapterApi.fromDto(saved);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @DELETE
    @Path("adapters/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_DELETE)
    @ApiOperation(value = "Delete the given data adapter", notes = "The data adapter cannot be in use by any lookup table, otherwise the request will fail.")
    public DataAdapterApi deleteAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<DataAdapterDto> dataAdapterDto = dbDataAdapterService.get(idOrName);
        if (!dataAdapterDto.isPresent()) {
            throw new NotFoundException();
        }
        DataAdapterDto dto = dataAdapterDto.get();
        checkPermission(RestPermissions.LOOKUP_TABLES_DELETE, dto.id());
        boolean unused = dbTableService.findByDataAdapterIds(singleton(dto.id())).isEmpty();
        if (!unused) {
            throw new BadRequestException("The adapter is still in use, cannot delete.");
        }
        dbDataAdapterService.delete(idOrName);

        return DataAdapterApi.fromDto(dto);
    }

    @PUT
    @Path("adapters/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_UPDATE)
    @ApiOperation(value = "Update the given data adapter settings")
    public DataAdapterApi updateAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                                        @Valid @ApiParam DataAdapterApi toUpdate) {
        checkLookupAdapterId(idOrName, toUpdate);
        checkPermission(RestPermissions.LOOKUP_TABLES_EDIT, toUpdate.id());
        DataAdapterDto saved = dbDataAdapterService.save(toUpdate.toDto());

        return DataAdapterApi.fromDto(saved);
    }

    @POST
    @Path("adapters/validate")
    @NoAuditEvent("Validation only")
    @ApiOperation(value = "Validate the data adapter config")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public ValidationResult validateAdapter(@Valid @ApiParam DataAdapterApi toValidate) {
        final ValidationResult validation = new ValidationResult();

        final Optional<DataAdapterDto> dtoOptional = dbDataAdapterService.get(toValidate.name());
        if (dtoOptional.isPresent()) {
            // an adapter exist with the given name, check that the IDs are the same, this might be an update
            final DataAdapterDto adapterDto = dtoOptional.get();
            //noinspection ConstantConditions
            if (!adapterDto.id().equals(toValidate.id())) {
                // an adapter exists with a different id, so the name is already in use, fail validation
                validation.addError("name", "The data adapter name is already in use.");
            }
        }

        final Optional<Multimap<String, String>> configValidations = toValidate.config()
                .validate(lookupDataAdapterValidationContext);
        configValidations.ifPresent(validation::addAll);

        return validation;
    }

    @JsonAutoDetect
    public static class DataAdapterPage {
        @Nullable
        @JsonProperty
        private final String query;

        @JsonUnwrapped
        private final PaginatedList.PaginationInfo paginationInfo;

        @JsonProperty("data_adapters")
        private final List<DataAdapterApi> dataAdapters;

        public DataAdapterPage(@Nullable String query, PaginatedList.PaginationInfo paginationInfo, List<DataAdapterApi> dataAdapters) {
            this.query = query;
            this.paginationInfo = paginationInfo;
            this.dataAdapters = dataAdapters;
        }
    }

    @GET
    @Path("caches")
    @ApiOperation(value = "List available caches")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public CachesPage caches(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                             @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                             @ApiParam(name = "sort",
                                     value = "The field to sort the result on",
                                     required = true,
                                     allowableValues = "title,description,name,id")
                             @DefaultValue(CacheDto.FIELD_TITLE) @QueryParam("sort") String sort,
                             @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                             @DefaultValue("desc") @QueryParam("order") String order,
                             @ApiParam(name = "query") @QueryParam("query") String query) {
        if (!CACHE_ALLOWABLE_SORT_FIELDS.contains(sort.toLowerCase(Locale.ENGLISH))) {
            sort = CacheDto.FIELD_TITLE;
        }
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(sort);
        } else {
            sortBuilder = DBSort.asc(sort);
        }

        try {
            final SearchQuery searchQuery = cacheSearchQueryParser.parse(query);
            final DBQuery.Query dbQuery = searchQuery.toDBQuery();


            PaginatedList<CacheDto> paginated = dbCacheService.findPaginated(dbQuery, sortBuilder, page, perPage);
            return new CachesPage(query,
                    paginated.pagination(),
                    paginated.stream().map(CacheApi::fromDto).collect(Collectors.toList()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @GET
    @Path("types/caches")
    @ApiOperation(value = "List available caches types")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public Map<String, LookupCache.Descriptor> availableCacheTypes() {
        return cacheTypes.values().stream()
                .map(LookupCache.Factory::getDescriptor)
                .collect(Collectors.toMap(LookupCache.Descriptor::getType, Function.identity()));

    }

    @GET
    @Path("caches/{idOrName}")
    @ApiOperation(value = "List the given cache")
    public CacheApi getCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<CacheDto> cacheDto = dbCacheService.get(idOrName);
        if (cacheDto.isPresent()) {
            checkPermission(RestPermissions.LOOKUP_TABLES_READ, cacheDto.get().id());
            return CacheApi.fromDto(cacheDto.get());
        }
        throw new NotFoundException();
    }

    @POST
    @Path("caches")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_CREATE)
    @ApiOperation(value = "Create a new cache")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_CREATE)
    public CacheApi createCache(@ApiParam CacheApi newCache) {
        try {
            final CacheDto saved = dbCacheService.save(newCache.toDto());
            return CacheApi.fromDto(saved);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @DELETE
    @Path("caches/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_DELETE)
    @ApiOperation(value = "Delete the given cache", notes = "The cache cannot be in use by any lookup table, otherwise the request will fail.")
    public CacheApi deleteCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<CacheDto> cacheDto = dbCacheService.get(idOrName);
        if (!cacheDto.isPresent()) {
            throw new NotFoundException();
        }
        CacheDto dto = cacheDto.get();
        checkPermission(RestPermissions.LOOKUP_TABLES_DELETE, dto.id());
        boolean unused = dbTableService.findByCacheIds(singleton(dto.id())).isEmpty();
        if (!unused) {
            throw new BadRequestException("The cache is still in use, cannot delete.");
        }
        dbCacheService.delete(idOrName);

        return CacheApi.fromDto(dto);
    }

    @PUT
    @Path("caches/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_UPDATE)
    @ApiOperation(value = "Update the given cache settings")
    public CacheApi updateCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                                @ApiParam CacheApi toUpdate) {
        checkLookupCacheId(idOrName, toUpdate);
        checkPermission(RestPermissions.LOOKUP_TABLES_EDIT, toUpdate.id());
        CacheDto saved = dbCacheService.save(toUpdate.toDto());
        return CacheApi.fromDto(saved);
    }

    @POST
    @Path("caches/validate")
    @NoAuditEvent("Validation only")
    @ApiOperation(value = "Validate the cache config")
    @RequiresPermissions(RestPermissions.LOOKUP_TABLES_READ)
    public ValidationResult validateCache(@Valid @ApiParam CacheApi toValidate) {
        final ValidationResult validation = new ValidationResult();

        final Optional<CacheDto> dtoOptional = dbCacheService.get(toValidate.name());
        if (dtoOptional.isPresent()) {
            // a cache exist with the given name, check that the IDs are the same, this might be an update
            final CacheDto cacheDto = dtoOptional.get();
            //noinspection ConstantConditions
            if (!cacheDto.id().equals(toValidate.id())) {
                // a ache exists with a different id, so the name is already in use, fail validation
                validation.addError("name", "The cache name is already in use.");
            }
        }

        final Optional<Multimap<String, String>> configValidations = toValidate.config().validate();
        configValidations.ifPresent(validation::addAll);

        return validation;
    }

    @JsonAutoDetect
    public static class CachesPage {
        @Nullable
        @JsonProperty
        private final String query;

        @JsonUnwrapped
        private final PaginatedList.PaginationInfo paginationInfo;

        @JsonProperty("caches")
        private final List<CacheApi> caches;

        public CachesPage(@Nullable String query, PaginatedList.PaginationInfo paginationInfo, List<CacheApi> caches) {
            this.query = query;
            this.paginationInfo = paginationInfo;
            this.caches = caches;
        }
    }
}
