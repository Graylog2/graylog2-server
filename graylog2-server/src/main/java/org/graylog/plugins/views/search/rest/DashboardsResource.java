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

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.bson.conversions.Bson;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import static java.util.Locale.ENGLISH;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Dashboards", tags = {CLOUD_VISIBLE})
@Produces(MediaType.APPLICATION_JSON)
@Path("/dashboards")
public class DashboardsResource extends RestResource {
    public enum Scope {
        @JsonProperty("read")
        READ,
        @JsonProperty("update")
        UPDATE;

        // Jersey will look for a #fromString method to deserialize a query parameter
        @JsonCreator
        public static Scope fromString(String scope) {
            return switch (scope.toLowerCase(Locale.ENGLISH)) {
                case "read" -> READ;
                case "update" -> UPDATE;
                // throwing an IllegalArgumentException here would have Jersey abort with a 404
                default -> throw new BadRequestException("Unknown scope: " + scope);
            };
        }
    }

    private final ViewService dbService;

    private static final String DEFAULT_SORT_FIELD = ViewDTO.FIELD_TITLE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("_id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_TITLE).title("Title").searchable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_CREATED_AT).title("Created").type(SearchQueryField.Type.DATE).filterable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_LAST_UPDATED_AT).title("Last Updated").type(SearchQueryField.Type.DATE).filterable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_DESCRIPTION).title("Description").searchable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_SUMMARY).title("Summary").searchable(true).build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_OWNER).title("Owner").build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_FAVORITE).title("Favorite").sortable(false).build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    private final DbQueryCreator dbQueryCreator;

    @Inject
    public DashboardsResource(final ViewService dbService) {
        this.dbService = dbService;
        this.dbQueryCreator = new DbQueryCreator(ViewDTO.FIELD_TITLE, attributes);
    }

    @GET
    @ApiOperation("Get a list of all dashboards")
    @Timed
    public PageListResponse<ViewSummaryDTO> views(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                  @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                  @ApiParam(name = "sort",
                                                            value = "The field to sort the result on",
                                                            required = true,
                                                            allowableValues = "id,title,created_at,description,summary,owner") @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sortField,
                                                  @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc") @DefaultValue("asc") @QueryParam("order") SortOrder order,
                                                  @ApiParam(name = "query") @QueryParam("query") String query,
                                                  @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                  @ApiParam(name = "scope",
                                                            value = "The scope of the permissions",
                                                            required = true,
                                                            allowableValues = "read,update") @DefaultValue("read") @QueryParam("scope") Scope scope,
                                                  @Context SearchUser searchUser) {

        final Predicate<ViewSummaryDTO> predicate = switch (scope) {
            case READ -> searchUser::canReadView;
            case UPDATE -> searchUser::canUpdateView;
        };

        if (!ViewDTO.SORT_FIELDS.contains(sortField.toLowerCase(ENGLISH))) {
            sortField = ViewDTO.FIELD_TITLE;
        }

        try {
            final Bson dbQuery = dbQueryCreator.createDbQuery(filters, query);
            final PaginatedList<ViewSummaryDTO> result = dbService.searchSummariesPaginatedByType(
                    searchUser,
                    ViewDTO.Type.DASHBOARD,
                    dbQuery,
                    predicate,
                    order,
                    sortField,
                    page,
                    perPage);

            return PageListResponse.create(query, result.pagination(), result.pagination().total(), sortField, order, result, attributes, settings);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
