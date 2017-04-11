package org.graylog2.rest.resources.system.lookup;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.lookup.MongoLutDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.rest.models.PaginatedList;
import org.graylog2.rest.models.system.lookup.DataAdapterApi;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
@Path("/system/lookup/adapters")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup/DataAdapters", description = "Lookup table data adapters")
public class LookupDataAdapterResource extends RestResource {

    private MongoLutDataAdapterService adapterService;

    @Inject
    public LookupDataAdapterResource(MongoLutDataAdapterService adapterService) {
        this.adapterService = adapterService;
    }

    @GET
    @ApiOperation(value = "List available data adapters")
    public DataAdapterPage list(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
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
    @Path("types")
    @ApiOperation(value = "List available data adapter types")
    public List<String> availableTypes() {
        return Collections.emptyList();
    }

    @GET
    @Path("{idOrName}")
    @ApiOperation(value = "List the given data adapter")
    public DataAdapterApi get(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        Optional<DataAdapterDto> dataAdapterDto = adapterService.get(idOrName);
        if (dataAdapterDto.isPresent()) {
            return DataAdapterApi.fromDto(dataAdapterDto.get());
        }
        throw new NotFoundException();
    }

    @POST
    @ApiOperation(value = "Create a new data adapter")
    public DataAdapterApi create(@ApiParam DataAdapterApi newAdapter) {
        DataAdapterDto dto = newAdapter.toDto();
        DataAdapterDto saved = adapterService.save(dto);
        return DataAdapterApi.fromDto(saved);
    }

    @DELETE
    @Path("{idOrName}")
    @ApiOperation(value = "Delete the given data adapter", notes = "The data adapter cannot be in use by any lookup table, otherwise the request will fail.")
    public void delete(@ApiParam(name = "idOrName") @PathParam("idOrName") @NotEmpty String idOrName) {
        // TODO validate that adapter isn't in use
        adapterService.delete(idOrName);
    }

    @PUT
    @ApiOperation(value = "Update the given data adapter settings")
    public DataAdapterApi update(@ApiParam DataAdapterApi toUpdate) {
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
}
