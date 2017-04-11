package org.graylog2.rest.resources.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.lookup.MongoLutCacheService;
import org.graylog2.lookup.MongoLutDataAdapterService;
import org.graylog2.lookup.MongoLutService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.rest.models.PaginatedList;
import org.graylog2.rest.models.system.lookup.CacheApi;
import org.graylog2.rest.models.system.lookup.DataAdapterApi;
import org.graylog2.rest.models.system.lookup.LookupTableApi;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
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

@RequiresAuthentication
@Path("/system/lookup")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup", description = "Lookup tables")
public class LookupTableResource extends RestResource {

    private final MongoLutService lookupTableService;
    private final MongoLutDataAdapterService adapterService;
    private final MongoLutCacheService cacheService;
    private final Map<String, LookupCache> cacheTypes;
    private final Map<String, LookupDataAdapter.Factory> dataAdapterTypes;

    @Inject
    public LookupTableResource(MongoLutService lookupTableService,
                               MongoLutDataAdapterService adapterService,
                               MongoLutCacheService cacheService,
                               Map<String, LookupCache> cacheTypes,
                               Map<String, LookupDataAdapter.Factory> dataAdapterTypes) {
        this.lookupTableService = lookupTableService;
        this.adapterService = adapterService;
        this.cacheService = cacheService;
        this.cacheTypes = cacheTypes;
        this.dataAdapterTypes = dataAdapterTypes;
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
                                  @ApiParam(name = "query") @QueryParam("query") String query) {

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
        return new LookupTablePage(query,
                paginated.pagination(),
                paginated.stream().map(LookupTableApi::fromDto).collect(Collectors.toList()));
    }

    @POST
    @Path("tables")
    @ApiOperation(value = "Create a new lookup table")
    public LookupTableApi createTable(@ApiParam LookupTableApi lookupTable) {
        LookupTableDto saved = lookupTableService.save(lookupTable.toDto());
        return LookupTableApi.fromDto(saved);
    }

    @DELETE
    @Path("tables/{idOrName}")
    @ApiOperation(value = "Delete the lookup table")
    public void removeTable(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that table isn't in use
        lookupTableService.delete(idOrName);
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

        public LookupTablePage(@Nullable String query, PaginatedList.PaginationInfo paginationInfo, List<LookupTableApi> lookupTables) {
            this.query = query;
            this.paginationInfo = paginationInfo;
            this.lookupTables = lookupTables;
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
    @ApiOperation(value = "Create a new data adapter")
    public DataAdapterApi createAdapter(@ApiParam DataAdapterApi newAdapter) {
        DataAdapterDto dto = newAdapter.toDto();
        DataAdapterDto saved = adapterService.save(dto);
        return DataAdapterApi.fromDto(saved);
    }

    @DELETE
    @Path("adapters/{idOrName}")
    @ApiOperation(value = "Delete the given data adapter", notes = "The data adapter cannot be in use by any lookup table, otherwise the request will fail.")
    public void deleteAdapter(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that adapter isn't in use
        adapterService.delete(idOrName);
    }

    @PUT
    @Path("adapters")
    @ApiOperation(value = "Update the given data adapter settings")
    public DataAdapterApi updateAdapter(@ApiParam DataAdapterApi toUpdate) {
        DataAdapterDto saved = adapterService.save(toUpdate.toDto());
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
    public Set<String> availableCacheTypes() {
        return cacheTypes.keySet();
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
    @ApiOperation(value = "Create a new cache")
    public CacheApi createCache(@ApiParam CacheApi newCache) {
        return CacheApi.fromDto(cacheService.save(newCache.toDto()));
    }

    @DELETE
    @Path("caches/{idOrName}")
    @ApiOperation(value = "Delete the given cache", notes = "The cache cannot be in use by any lookup table, otherwise the request will fail.")
    public void deleteCache(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that cache isn't in use
        cacheService.delete(idOrName);
    }

    @PUT
    @Path("caches")
    @ApiOperation(value = "Update the given cache settings")
    public CacheApi updateCache(@ApiParam CacheApi toUpdate) {
        return CacheApi.fromDto(cacheService.save(toUpdate.toDto()));
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
