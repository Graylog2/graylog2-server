/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.lookup;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.lookup.LookupTable;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.lookup.MongoLutCacheService;
import org.graylog2.lookup.MongoLutDataAdapterService;
import org.graylog2.lookup.MongoLutService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.lookup.events.LookupTablesDeleted;
import org.graylog2.lookup.events.LookupTablesUpdated;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.rest.models.PaginatedList;
import org.graylog2.rest.models.system.lookup.CacheApi;
import org.graylog2.rest.models.system.lookup.DataAdapterApi;
import org.graylog2.rest.models.system.lookup.ErrorStates;
import org.graylog2.rest.models.system.lookup.ErrorStatesRequest;
import org.graylog2.rest.models.system.lookup.LookupTableApi;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.validation.Valid;
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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import static java.util.Collections.singleton;
import static org.slf4j.LoggerFactory.getLogger;

@RequiresAuthentication
@Path("/system/lookup")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup", description = "Lookup tables")
public class LookupTableResource extends RestResource {
    private static final Logger LOG = getLogger(LookupTableResource.class);

    private final MongoLutService lookupTableService;
    private final MongoLutDataAdapterService adapterService;
    private final MongoLutCacheService cacheService;
    private final Map<String, LookupCache.Factory> cacheTypes;
    private final Map<String, LookupDataAdapter.Factory> dataAdapterTypes;
    private LookupTableService lookupTables;
    private ClusterEventBus clusterBus;

    @Inject
    public LookupTableResource(MongoLutService lookupTableService,
                               MongoLutDataAdapterService adapterService,
                               MongoLutCacheService cacheService,
                               Map<String, LookupCache.Factory> cacheTypes,
                               Map<String, LookupDataAdapter.Factory> dataAdapterTypes,
                               LookupTableService lookupTables,
                               ClusterEventBus clusterBus) {
        this.lookupTableService = lookupTableService;
        this.adapterService = adapterService;
        this.cacheService = cacheService;
        this.cacheTypes = cacheTypes;
        this.dataAdapterTypes = dataAdapterTypes;
        this.lookupTables = lookupTables;
        this.clusterBus = clusterBus;
    }

    @GET
    @Path("data/{name}")
    @ApiOperation(value = "Query a lookup table")
    public Object performLookup(@ApiParam(name = "name") @PathParam("name") @NotEmpty String name,
                                @ApiParam(name = "key") @QueryParam("key") @NotEmpty String key) {
        return lookupTables.newBuilder().lookupTable(name).build().lookup(key);
    }

    @GET
    @Path("tables")
    @ApiOperation(value = "List configured lookup tables")
    public LookupTablePage tables(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                  @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                  @ApiParam(name = "sort",
                                          value = "The field to sort the result on",
                                          required = true,
                                          allowableValues = "title")
                                  @DefaultValue("created_at") @QueryParam("sort") String sort,
                                  @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                  @DefaultValue("desc") @QueryParam("order") String order,
                                  @ApiParam(name = "query") @QueryParam("query") String query,
                                  @ApiParam(name = "resolve") @QueryParam("resolve") @DefaultValue("false") boolean resolveObjects) {

        // TODO hoist SearchQueryParser for this
        DBQuery.Query dbQuery = DBQuery.empty();

        // TODO determine sortable fields
        sort = "title";
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(sort);
        } else {
            sortBuilder = DBSort.asc(sort);
        }

        PaginatedList<LookupTableDto> paginated = lookupTableService.findPaginated(dbQuery, sortBuilder, page, perPage);

        ImmutableSet.Builder<CacheApi> caches = ImmutableSet.builder();
        ImmutableSet.Builder<DataAdapterApi> dataAdapters = ImmutableSet.builder();
        if (resolveObjects) {
            ImmutableSet.Builder<String> cacheIds = ImmutableSet.builder();
            ImmutableSet.Builder<String> dataAdapterIds = ImmutableSet.builder();

            paginated.forEach(dto -> {
                cacheIds.add(dto.cacheId());
                dataAdapterIds.add(dto.dataAdapterId());
            });

            cacheService.findByIds(cacheIds.build()).forEach(cacheDto -> caches.add(CacheApi.fromDto(cacheDto)));
            adapterService.findByIds(dataAdapterIds.build()).forEach(dataAdapterDto -> dataAdapters.add(DataAdapterApi.fromDto(dataAdapterDto)));
        }

        return new LookupTablePage(query,
                paginated.pagination(),
                paginated.stream().map(LookupTableApi::fromDto).collect(Collectors.toList()),
                caches.build(),
                dataAdapters.build());
    }

    @GET
    @Path("tables/{idOrName}")
    @ApiOperation(value = "Retrieve the named lookup table")
    public LookupTablePage get(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName,
                               @ApiParam(name = "resolve") @QueryParam("resolve") @DefaultValue("false") boolean resolveObjects) {

        Optional<LookupTableDto> lookupTableDto = lookupTableService.get(idOrName);
        if (!lookupTableDto.isPresent()) {
            throw new NotFoundException();
        }
        LookupTableDto tableDto = lookupTableDto.get();

        Set<CacheApi> caches = Collections.emptySet();
        Set<DataAdapterApi> adapters = Collections.emptySet();

        if (resolveObjects) {
            caches = cacheService.findByIds(Collections.singleton(tableDto.cacheId())).stream().map(CacheApi::fromDto).collect(Collectors.toSet());
            adapters = adapterService.findByIds(Collections.singleton(tableDto.dataAdapterId())).stream().map(DataAdapterApi::fromDto).collect(Collectors.toSet());
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
    public LookupTableApi createTable(@ApiParam LookupTableApi lookupTable) {
        LookupTableDto saved = lookupTableService.save(lookupTable.toDto());
        LookupTableApi table = LookupTableApi.fromDto(saved);

        clusterBus.post(LookupTablesUpdated.create(saved));

        return table;
    }

    @PUT
    @Path("tables")
    @AuditEvent(type = AuditEventTypes.LOOKUP_TABLE_UPDATE)
    @ApiOperation(value = "Update the given lookup table")
    public LookupTableApi updateTable(@Valid @ApiParam LookupTableApi toUpdate) {
        LookupTableDto saved = lookupTableService.save(toUpdate.toDto());
        clusterBus.post(LookupTablesUpdated.create(saved));

        return LookupTableApi.fromDto(saved);
    }

    @DELETE
    @Path("tables/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_TABLE_DELETE)
    @ApiOperation(value = "Delete the lookup table")
    public LookupTableApi removeTable(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that table isn't in use, how?
        Optional<LookupTableDto> lookupTableDto = lookupTableService.get(idOrName);
        if (!lookupTableDto.isPresent()) {
            throw new NotFoundException();
        }
        lookupTableService.delete(idOrName);
        clusterBus.post(LookupTablesDeleted.create(lookupTableDto.get()));

        return LookupTableApi.fromDto(lookupTableDto.get());
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
    public DataAdapterPage adapters(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                @ApiParam(name = "sort",
                                        value = "The field to sort the result on",
                                        required = true,
                                        allowableValues = "title")
                                @DefaultValue("title") @QueryParam("sort") String sort,
                                @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                @DefaultValue("desc") @QueryParam("order") String order,
                                @ApiParam(name = "query") @QueryParam("query") String query) {

        PaginatedList<DataAdapterDto> paginated = adapterService.findPaginated(DBQuery.empty(), DBSort.asc(sort), page, perPage);
        return new DataAdapterPage(query,
                paginated.pagination(),
                paginated.stream().map(DataAdapterApi::fromDto).collect(Collectors.toList()));
    }

    @GET
    @Path("types/adapters")
    @ApiOperation(value = "List available data adapter types")
    public Map<String, LookupDataAdapter.Descriptor> availableAdapterTypes() {

        return dataAdapterTypes.values().stream()
                .map(LookupDataAdapter.Factory::getDescriptor)
                .collect(Collectors.toMap(LookupDataAdapter.Descriptor::getType, Function.identity()));

    }

    @POST
    @NoAuditEvent("Bulk read call")
    @Path("states")
    @ApiOperation(value = "Retrieve the runtime error states of the given lookup tables")
    public ErrorStates adapterErrorStates(@ApiParam(name = "tables") @Valid ErrorStatesRequest request) {
        final ErrorStates.Builder errorStates = ErrorStates.builder();
        if (request.tables() != null) {
            //noinspection ConstantConditions
            for (String tableName : request.tables()) {

                final LookupTable table = lookupTables.newBuilder().lookupTable(tableName).build().getTable();
                if (table != null) {
                    errorStates.tables().put(tableName, table.error());
                }
            }
        }
        lookupTables.getDataAdapters(request.dataAdapters()).forEach(adapter -> {
            errorStates.dataAdapters().put(adapter.name(), adapter.getError().map(Throwable::getMessage).orElse(null));
        });
        return errorStates.build();
    }

    @GET
    @Path("adapters/{idOrName}")
    @ApiOperation(value = "List the given data adapter")
    public DataAdapterApi getAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<DataAdapterDto> dataAdapterDto = adapterService.get(idOrName);
        if (dataAdapterDto.isPresent()) {
            return DataAdapterApi.fromDto(dataAdapterDto.get());
        }
        throw new NotFoundException();
    }

    @POST
    @Path("adapters")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_CREATE)
    @ApiOperation(value = "Create a new data adapter")
    public DataAdapterApi createAdapter(@Valid @ApiParam DataAdapterApi newAdapter) {
        DataAdapterDto dto = newAdapter.toDto();
        DataAdapterDto saved = adapterService.save(dto);
        return DataAdapterApi.fromDto(saved);
    }

    @DELETE
    @Path("adapters/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_DELETE)
    @ApiOperation(value = "Delete the given data adapter", notes = "The data adapter cannot be in use by any lookup table, otherwise the request will fail.")
    public DataAdapterApi deleteAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<DataAdapterDto> dataAdapterDto = adapterService.get(idOrName);
        if (!dataAdapterDto.isPresent()) {
            throw new NotFoundException();
        }
        DataAdapterDto dto = dataAdapterDto.get();
        boolean unused = lookupTableService.findByDataAdapterIds(singleton(dto.id())).isEmpty();
        if (!unused) {
            throw new BadRequestException("The adapter is still in use, cannot delete.");
        }
        adapterService.delete(idOrName);

        return DataAdapterApi.fromDto(dto);
    }

    @PUT
    @Path("adapters")
    @AuditEvent(type = AuditEventTypes.LOOKUP_ADAPTER_UPDATE)
    @ApiOperation(value = "Update the given data adapter settings")
    public DataAdapterApi updateAdapter(@Valid @ApiParam DataAdapterApi toUpdate) {
        DataAdapterDto saved = adapterService.save(toUpdate.toDto());
        Collection<LookupTableDto> adapterUsages = lookupTableService.findByDataAdapterIds(singleton(saved.id()));
        if (!adapterUsages.isEmpty()) {
            clusterBus.post(LookupTablesUpdated.create(adapterUsages));
        }
        return DataAdapterApi.fromDto(saved);
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
    public CachesPage caches(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                             @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                             @ApiParam(name = "sort",
                                     value = "The field to sort the result on",
                                     required = true,
                                     allowableValues = "title")
                             @DefaultValue("created_at") @QueryParam("sort") String sort,
                             @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                             @DefaultValue("desc") @QueryParam("order") String order,
                             @ApiParam(name = "query") @QueryParam("query") String query) {
        PaginatedList<CacheDto> paginated = cacheService.findPaginated(DBQuery.empty(), DBSort.asc(sort), page, perPage);
        return new CachesPage(query,
                paginated.pagination(),
                paginated.stream().map(CacheApi::fromDto).collect(Collectors.toList()));
    }

    @GET
    @Path("types/caches")
    @ApiOperation(value = "List available caches types")
    public Map<String, LookupCache.Descriptor> availableCacheTypes() {
        return cacheTypes.values().stream()
                .map(LookupCache.Factory::getDescriptor)
                .collect(Collectors.toMap(LookupCache.Descriptor::getType, Function.identity()));

    }

    @GET
    @Path("caches/{idOrName}")
    @ApiOperation(value = "List the given cache")
    public CacheApi getCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<CacheDto> cacheDto = cacheService.get(idOrName);
        if (cacheDto.isPresent()) {
            return CacheApi.fromDto(cacheDto.get());
        }
        throw new NotFoundException();
    }

    @POST
    @Path("caches")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_CREATE)
    @ApiOperation(value = "Create a new cache")
    public CacheApi createCache(@ApiParam CacheApi newCache) {
        return CacheApi.fromDto(cacheService.save(newCache.toDto()));
    }

    @DELETE
    @Path("caches/{idOrName}")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_DELETE)
    @ApiOperation(value = "Delete the given cache", notes = "The cache cannot be in use by any lookup table, otherwise the request will fail.")
    public CacheApi deleteCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<CacheDto> cacheDto = cacheService.get(idOrName);
        if (!cacheDto.isPresent()) {
            throw new NotFoundException();
        }
        CacheDto dto = cacheDto.get();
        boolean unused = lookupTableService.findByCacheIds(singleton(dto.id())).isEmpty();
        if (!unused) {
            throw new BadRequestException("The cache is still in use, cannot delete.");
        }
        cacheService.delete(idOrName);

        return CacheApi.fromDto(dto);
    }

    @PUT
    @Path("caches")
    @AuditEvent(type = AuditEventTypes.LOOKUP_CACHE_UPDATE)
    @ApiOperation(value = "Update the given cache settings")
    public CacheApi updateCache(@ApiParam CacheApi toUpdate) {
        CacheDto saved = cacheService.save(toUpdate.toDto());
        Collection<LookupTableDto> cacheUsages = lookupTableService.findByCacheIds(singleton(saved.id()));
        if (!cacheUsages.isEmpty()) {
            clusterBus.post(LookupTablesUpdated.create(cacheUsages));
        }
        return CacheApi.fromDto(saved);
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
