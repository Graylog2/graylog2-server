package org.graylog2.rest.resources.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.lookup.MongoLutService;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.rest.models.PaginatedList;
import org.graylog2.rest.models.system.lookup.LookupTableApi;
import org.graylog2.shared.rest.resources.RestResource;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RequiresAuthentication
@Path("/system/lookup/tables")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup/Tables", description = "Lookup tables")
public class LookupTableResource extends RestResource {

    private MongoLutService lookupTableService;

    @Inject
    public LookupTableResource(MongoLutService lookupTableService) {
        this.lookupTableService = lookupTableService;
    }

    @GET
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
    @ApiOperation(value = "Create a new lookup table")
    public LookupTableApi createTable(@ApiParam LookupTableApi lookupTable) {
        LookupTableDto saved = lookupTableService.save(lookupTable.toDto());
        return LookupTableApi.fromDto(saved);
    }

    @DELETE
    @Path("{id}")
    @ApiOperation(value = "Delete the lookup table")
    public void removeTable(@PathParam("id") String idOrName) {
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
}
