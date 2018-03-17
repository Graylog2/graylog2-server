package org.graylog.plugins.enterprise.search.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.enterprise.database.PaginatedList;
import org.graylog.plugins.enterprise.search.views.ViewDTO;
import org.graylog.plugins.enterprise.search.views.ViewService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Locale.ENGLISH;

// TODO permission system
@Api(value = "Enterprise/Views", description = "Views management")
@Path("/views")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ViewsResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ViewsResource.class);
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
            // TODO: Add permission checks - need to check every view for permissions
            final PaginatedList<ViewDTO> result = dbService.searchPaginated(searchQuery, order, sort, page, perPage);

            return PaginatedResponse.create("views", result, query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{id}")
    @ApiOperation("Get a single view")
    public ViewDTO get(@ApiParam @PathParam("id") @NotEmpty String id) {
        if ("default".equals(id)) {
            // If the user is not permitted to access the default view, return a 404
            return dbService.getDefault()
                    .filter(dto -> isPermitted(EnterpriseSearchRestPermissions.VIEW_READ, dto.id()))
                    .orElseThrow(() -> new NotFoundException("Default view doesn't exist"));
        }
        checkPermission(EnterpriseSearchRestPermissions.VIEW_READ, id);
        return loadView(id);
    }

    @POST
    @ApiOperation("Create a new view")
    @RequiresPermissions(EnterpriseSearchRestPermissions.VIEW_CREATE)
    public ViewDTO create(@ApiParam @Valid ViewDTO dto) throws ValidationException {
        final ViewDTO savedDto = dbService.save(dto);
        ensureUserPermissions(savedDto);
        return savedDto;
    }

    @PUT
    @Path("{id}")
    @ApiOperation("Update view")
    public ViewDTO update(@ApiParam @PathParam("id") @NotEmpty String id,
                          @ApiParam @Valid ViewDTO dto) {
        checkPermission(EnterpriseSearchRestPermissions.VIEW_EDIT, id);
        loadView(id);
        return dbService.save(dto.toBuilder().id(id).build());
    }

    @PUT
    @Path("{id}/default")
    @ApiOperation("Configures the view as default view")
    public void setDefault(@ApiParam @PathParam("id") @NotEmpty String id) {
        checkPermission(EnterpriseSearchRestPermissions.VIEW_READ, id);
        checkPermission(EnterpriseSearchRestPermissions.DEFAULT_VIEW_SET);
        dbService.saveDefault(loadView(id));
    }

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete view")
    public ViewDTO delete(@ApiParam @PathParam("id") @NotEmpty String id) {
        checkPermission(EnterpriseSearchRestPermissions.VIEW_DELETE, id);
        final ViewDTO dto = loadView(id);
        dbService.delete(id);
        removeUserPermissions(dto);
        return dto;
    }

    private ViewDTO loadView(String id) {
        return dbService.get(id).orElseThrow(() -> new NotFoundException("View " + id + " doesn't exist"));
    }

    private void ensureUserPermissions(ViewDTO dto) throws ValidationException {
        final User user = getCurrentUser();
        if (user != null && !user.isLocalAdmin()) {
            final List<String> permissions = ImmutableList.<String>builder()
                    .addAll(user.getPermissions())
                    .addAll(getViewPermissions(dto))
                    .build();
            user.setPermissions(permissions);
            userService.save(user);
        }
    }

    // TODO: Should be moved to org.graylog2.users.UserPermissionsCleanupListener once view are merged into the server
    private void removeUserPermissions(ViewDTO dto) {
        userService.loadAll().forEach(user -> {
            final List<String> newPermissions = new ArrayList<>(user.getPermissions());
            boolean modifiedPermissions = newPermissions.removeAll(getViewPermissions(dto));

            if (modifiedPermissions) {
                user.setPermissions(newPermissions);
                try {
                    userService.save(user);
                    LOG.debug("Successfully updated permissions of user <{}>: {}", user.getName(), newPermissions);
                } catch (ValidationException e) {
                    LOG.warn("Unable to save user <{}> while removing permissions of deleted dashboard: ", user.getName(), e);
                }
            }
        });
    }

    private Set<String> getViewPermissions(ViewDTO dto) {
        if (isNullOrEmpty(dto.id())) {
            throw new IllegalArgumentException("ViewDTO needs an ID to create permissions");
        }
        return ImmutableSet.of(
                EnterpriseSearchRestPermissions.VIEW_READ + ":" + dto.id(),
                EnterpriseSearchRestPermissions.VIEW_EDIT + ":" + dto.id()
        );
    }
}
