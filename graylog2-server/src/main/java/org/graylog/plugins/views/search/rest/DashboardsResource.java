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
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.plugins.views.search.views.ViewSummaryDTO;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Locale;

import static java.util.Locale.ENGLISH;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Dashboards", tags = {CLOUD_VISIBLE})
@Produces(MediaType.APPLICATION_JSON)
@Path("/dashboards")
public class DashboardsResource extends RestResource {
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("title", SearchQueryField.create(ViewDTO.FIELD_TITLE))
            .put("description", SearchQueryField.create(ViewDTO.FIELD_DESCRIPTION))
            .put("summary", SearchQueryField.create(ViewDTO.FIELD_DESCRIPTION))
            .build();
    private final ViewService dbService;
    private final SearchQueryParser searchQueryParser;

    private static final String DEFAULT_SORT_FIELD = ViewDTO.FIELD_TITLE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id(ViewDTO.FIELD_TITLE).title("Title").build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_CREATED_AT).title("Created").type("date").build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_DESCRIPTION).title("Description").build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_SUMMARY).title("Summary").build(),
            EntityAttribute.builder().id(ViewDTO.FIELD_OWNER).title("Owner").build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    @Inject
    public DashboardsResource(ViewService dbService) {
        this.dbService = dbService;
        this.searchQueryParser = new SearchQueryParser(ViewDTO.FIELD_TITLE, SEARCH_FIELD_MAPPING);
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
                                                  @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc") @DefaultValue("asc") @QueryParam("order") String order,
                                                  @ApiParam(name = "query") @QueryParam("query") String query,
                                                  @Context SearchUser searchUser) {

        if (!ViewDTO.SORT_FIELDS.contains(sortField.toLowerCase(ENGLISH))) {
            sortField = ViewDTO.FIELD_TITLE;
        }

        try {
            final SearchQuery searchQuery = searchQueryParser.parse(query);
            final PaginatedList<ViewSummaryDTO> result = dbService.searchSummariesPaginatedByType(
                    searchUser,
                    ViewDTO.Type.DASHBOARD,
                    searchQuery,
                    searchUser::canReadView,
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
