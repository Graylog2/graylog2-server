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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchDomain;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityCheckStatus;
import org.graylog.plugins.views.search.searchfilters.db.SearchFilterVisibilityChecker;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewResolver;
import org.graylog.plugins.views.search.views.ViewResolverDecoder;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.WidgetDTO;
import org.graylog.security.UserContext;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.dashboards.events.DashboardDeletedEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;

@Api(value = "Views")
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
    private final ClusterEventBus clusterEventBus;
    private final SearchDomain searchDomain;
    private final Map<String, ViewResolver> viewResolvers;
    private final SearchFilterVisibilityChecker searchFilterVisibilityChecker;

    @Inject
    public ViewsResource(ViewService dbService,
                         ClusterEventBus clusterEventBus, SearchDomain searchDomain,
                         Map<String, ViewResolver> viewResolvers,
                         SearchFilterVisibilityChecker searchFilterVisibilityChecker) {
        this.dbService = dbService;
        this.clusterEventBus = clusterEventBus;
        this.searchDomain = searchDomain;
        this.viewResolvers = viewResolvers;
        this.searchQueryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, SEARCH_FIELD_MAPPING);
        this.searchFilterVisibilityChecker = searchFilterVisibilityChecker;
    }

    @GET
    @ApiOperation("Get a list of all views")
    public PaginatedResponse<ViewDTO> views(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                            @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                            @ApiParam(name = "sort",
                                                      value = "The field to sort the result on",
                                                      required = true,
                                                      allowableValues = "id,title,created_at") @DefaultValue(ViewDTO.FIELD_TITLE) @QueryParam("sort") String sortField,
                                            @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc") @DefaultValue("asc") @QueryParam("order") String order,
                                            @ApiParam(name = "query") @QueryParam("query") String query,
                                            @Context SearchUser searchUser) {

        if (!ViewDTO.SORT_FIELDS.contains(sortField.toLowerCase(ENGLISH))) {
            sortField = ViewDTO.FIELD_TITLE;
        }

        try {
            final SearchQuery searchQuery = searchQueryParser.parse(query);
            final PaginatedList<ViewDTO> result = dbService.searchPaginated(
                    searchQuery,
                    searchUser::canReadView,
                    order,
                    sortField,
                    page,
                    perPage);

            return PaginatedResponse.create("views", result, query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    @GET
    @Path("{id}")
    @ApiOperation("Get a single view")
    public ViewDTO get(@ApiParam(name = "id") @PathParam("id") @NotEmpty String id, @Context SearchUser searchUser) {
        if ("default".equals(id)) {
            // If the user is not permitted to access the default view, return a 404
            return dbService.getDefault()
                    .filter(searchUser::canReadView)
                    .orElseThrow(() -> new NotFoundException("Default view doesn't exist"));
        }

        // Attempt to resolve the view from optional view resolvers before using the default database lookup.
        // The view resolvers must be used first, because the ID may not be a valid hex ID string.
        ViewDTO view = resolveView(id);
        if (searchUser.canReadView(view)) {
            return view;
        }

        throw viewNotFoundException(id);
    }

    /**
     * Resolve (find) view from either the corresponding view resolver, or from the database.
     *
     * @param id The id of a view. If an ID matching the resolver format is provided (e.g. resolver_name:id)
     *           then a view will be looked up from the corresponding resolver, otherwise, it will be looked
     *           up in the database.
     * @return An optional view.
     */
    private ViewDTO resolveView(String id) {
        final ViewResolverDecoder decoder = new ViewResolverDecoder(id);
        if (decoder.isResolverViewId()) {
            final ViewResolver viewResolver = viewResolvers.get(decoder.getResolverName());
            if (viewResolver != null) {
                return viewResolver.get(decoder.getViewId())
                        .orElseThrow(() -> new NotFoundException("Failed to resolve view:" + id));
            } else {
                throw new NotFoundException("Failed to find view resolver: " + decoder.getResolverName());
            }
        } else {
            return loadView(id);
        }
    }


    @POST
    @ApiOperation("Create a new view")
    @AuditEvent(type = ViewsAuditEventTypes.VIEW_CREATE)
    public ViewDTO create(@ApiParam @Valid @NotNull(message = "View is mandatory") ViewDTO dto,
                          @Context UserContext userContext,
                          @Context SearchUser searchUser) throws ValidationException {
        if (dto.type().equals(ViewDTO.Type.DASHBOARD) && !searchUser.canCreateDashboards()) {
            throw new ForbiddenException("User is not allowed to create new dashboards.");
        }

        validateIntegrity(dto, searchUser);

        final User user = userContext.getUser();
        return dbService.saveWithOwner(dto.toBuilder().owner(searchUser.username()).build(), user);
    }

    private void validateIntegrity(ViewDTO dto, SearchUser searchUser) {
        final Search search = searchDomain.getForUser(dto.searchId(), searchUser)
                .orElseThrow(() -> new BadRequestException("Search " + dto.searchId() + " not available"));

        final Set<String> searchQueries = search.queries().stream()
                .map(Query::id)
                .collect(Collectors.toSet());

        final Set<String> stateQueries = dto.state().keySet();

        if(!searchQueries.containsAll(stateQueries)) {
            final Sets.SetView<String> diff = Sets.difference(searchQueries, stateQueries);
            throw new BadRequestException("Search queries do not correspond to view/state queries, missing query IDs: " + diff);
        }

        final Set<String> searchTypes = search.queries().stream()
                .flatMap(q -> q.searchTypes().stream())
                .map(SearchType::id)
                .collect(Collectors.toSet());


        final Set<String> stateTypes = dto.state().values().stream()
                .flatMap(v -> v.widgetMapping().values().stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        if(!searchTypes.containsAll(stateTypes)) {
            final Sets.SetView<String> diff = Sets.difference(searchTypes, stateTypes);
            throw new BadRequestException("Search types do not correspond to view/search types, missing searches: " + diff);
        }

        final Set<String> widgetIds = dto.state().values().stream()
                .flatMap(v -> v.widgets().stream())
                .map(WidgetDTO::id)
                .collect(Collectors.toSet());

        final Set<String> widgetPositions = dto.state().values().stream()
                .flatMap(v -> v.widgetPositions().keySet().stream()).collect(Collectors.toSet());

        if (!widgetPositions.containsAll(widgetIds)) {
            final Sets.SetView<String> diff = Sets.difference(widgetPositions, widgetIds);
            throw new BadRequestException("Widget positions don't correspond to widgets, missing widget possitions: " + diff);
        }

        final SearchFilterVisibilityCheckStatus searchFilterVisibilityCheckStatus = searchFilterVisibilityChecker.checkSearchFilterVisibility(
                filterID -> isPermitted(RestPermissions.SEARCH_FILTERS_READ, filterID), search);
        if (!searchFilterVisibilityCheckStatus.allSearchFiltersVisible()) {
            throw new BadRequestException(searchFilterVisibilityCheckStatus.toMessage());
        }
    }

    @PUT
    @Path("{id}")
    @ApiOperation("Update view")
    @AuditEvent(type = ViewsAuditEventTypes.VIEW_UPDATE)
    public ViewDTO update(@ApiParam(name = "id") @PathParam("id") @NotEmpty String id,
                          @ApiParam @Valid ViewDTO dto,
                          @Context SearchUser searchUser) {
        final ViewDTO updatedDTO = dto.toBuilder().id(id).build();
        if (!searchUser.canUpdateView(updatedDTO)) {
            throw new ForbiddenException("Not allowed to edit " + summarize(updatedDTO) + ".");
        }

        validateIntegrity(updatedDTO, searchUser);

        return dbService.update(updatedDTO);
    }

    @PUT
    @Path("{id}/default")
    @ApiOperation("Configures the view as default view")
    @AuditEvent(type = ViewsAuditEventTypes.DEFAULT_VIEW_SET)
    public void setDefault(@ApiParam(name = "id") @PathParam("id") @NotEmpty String id) {
        checkPermission(ViewsRestPermissions.VIEW_READ, id);
        checkPermission(ViewsRestPermissions.DEFAULT_VIEW_SET);
        dbService.saveDefault(loadView(id));
    }

    @DELETE
    @Path("{id}")
    @ApiOperation("Delete view")
    @AuditEvent(type = ViewsAuditEventTypes.VIEW_DELETE)
    public ViewDTO delete(@ApiParam(name = "id") @PathParam("id") @NotEmpty String id,
                          @Context SearchUser searchUser) {
        final ViewDTO view = loadView(id);
        if (!searchUser.canDeleteView(view)) {
            throw new ForbiddenException("Unable to delete " + summarize(view) + ".");
        }

        dbService.delete(id);
        triggerDeletedEvent(view);
        return view;
    }

    private String summarize(ViewDTO view) {
        return view.type().toString().toLowerCase(Locale.ROOT) + " <" + view.id() + ">";
    }

    private void triggerDeletedEvent(ViewDTO dto) {
        if (dto != null && dto.type() != null && dto.type().equals(ViewDTO.Type.DASHBOARD)) {
            final DashboardDeletedEvent dashboardDeletedEvent = DashboardDeletedEvent.create(dto.id());
            //noinspection UnstableApiUsage
            clusterEventBus.post(dashboardDeletedEvent);
        }
    }

    private ViewDTO loadView(String id) {
        try {
            return dbService.get(id).orElseThrow(() -> viewNotFoundException(id));
        } catch (IllegalArgumentException ignored) {
            throw viewNotFoundException(id);
        }
    }

    private NotFoundException viewNotFoundException(String id) {
        return new NotFoundException("View " + id + " doesn't exist");
    }
}
