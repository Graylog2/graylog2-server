package org.graylog2.rest.resources.system.lookup;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.lookup.MongoLutCacheService;
import org.graylog2.lookup.dto.CacheDto;
import org.graylog2.rest.models.PaginatedList;
import org.graylog2.rest.models.system.lookup.CacheApi;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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
@Path("/system/lookup/caches")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup/Caches", description = "Lookup table caches")
public class LookupCacheResource extends RestResource {

    private MongoLutCacheService cacheService;

    @Inject
    public LookupCacheResource(MongoLutCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GET
    @ApiOperation(value = "List available caches")
    public CachesPage list(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
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
    @Path("types")
    @ApiOperation(value = "List available caches types")
    public List<String> availableTypes() {
        return Collections.emptyList();
    }

    @GET
    @Path("{idOrName}")
    @ApiOperation(value = "List the given cache")
    public CacheApi get(@ApiParam("idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        return CacheApi.fromDto(cacheService.get(idOrName));
    }

    @POST
    @ApiOperation(value = "Create a new cache")
    public CacheApi create(@ApiParam CacheApi newCache) {
        return CacheApi.fromDto(cacheService.save(newCache.toDto()));
    }

    @DELETE
    @Path("{idOrName}")
    @ApiOperation(value = "Delete the given cache", notes = "The cache cannot be in use by any lookup table, otherwise the request will fail.")
    public void delete(@ApiParam("idOrName") @PathParam("idOrName") String idOrName) {
        cacheService.delete(idOrName);
    }

    @PUT
    @ApiOperation(value = "Update the given cache settings")
    public CacheApi update(@ApiParam CacheApi toUpdate) {
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
