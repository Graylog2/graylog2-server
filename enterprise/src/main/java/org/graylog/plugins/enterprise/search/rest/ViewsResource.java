package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.enterprise.database.PaginatedList;
import org.graylog.plugins.enterprise.search.views.ViewDTO;
import org.graylog.plugins.enterprise.search.views.ViewService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.BadRequestException;
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

import static java.util.Locale.ENGLISH;

// TODO permission system
@Api(value = "Enterprise/Views", description = "Views management")
@Path("/views")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ViewsResource extends RestResource implements PluginRestResource {
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(ViewDTO.FIELD_ID))
            .put("title", SearchQueryField.create(ViewDTO.FIELD_TITLE))
            .put("summary", SearchQueryField.create(ViewDTO.FIELD_DESCRIPTION))
            .build();

    private final ViewService dbService;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public ViewsResource(ViewService dbService) {
        this.dbService = dbService;
        this.searchQueryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, SEARCH_FIELD_MAPPING);
    }

    @GET
    @ApiOperation("Get a list of all views")
    public PaginatedResponse<ViewDTO> views(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                            @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                            @ApiParam(name = "sort",
                                                    value = "The field to sort the result on",
                                                    required = true,
                                                    allowableValues = "id,title,created_at") @DefaultValue(ViewDTO.FIELD_TITLE) @QueryParam("sort") String sort,
                                            @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc") @DefaultValue("asc") @QueryParam("order") String order,
                                            @ApiParam(name = "query") @QueryParam("query") String query ) {

        if (!ViewDTO.SORT_FIELDS.contains(sort.toLowerCase(ENGLISH))) {
            sort = ViewDTO.FIELD_TITLE;
        }

        try {
            final SearchQuery searchQuery = searchQueryParser.parse(query);
            final PaginatedList<ViewDTO> result = dbService.searchPaginated(searchQuery, order, sort, page, perPage);

            return PaginatedResponse.create("views", result, query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @POST
    @ApiOperation("Create a new view")
    public ViewDTO create(@ApiParam @Valid ViewDTO dto) {
        return dbService.save(dto);
    }

    @PUT
    @Path("{id}")
    @ApiOperation("Update view")
    public ViewDTO update(@ApiParam @PathParam("id") @NotEmpty String id,
                          @ApiParam @Valid ViewDTO dto) {
        loadView(id);
        return dbService.save(dto.toBuilder().id(id).build());
    }

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete view")
    public ViewDTO delete(@ApiParam @PathParam("id") @NotEmpty String id) {
        final ViewDTO dto = loadView(id);
        dbService.delete(id);
        return dto;
    }

    private ViewDTO loadView(String id) {
        return dbService.get(id).orElseThrow(() -> new NotFoundException("View " + id + " doesn't exist"));
    }
}
