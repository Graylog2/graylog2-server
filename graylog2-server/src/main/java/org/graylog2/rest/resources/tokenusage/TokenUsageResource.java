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
package org.graylog2.rest.resources.tokenusage;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.security.AccessTokenEntity;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.tokenusage.TokenUsageService;
import org.graylog2.users.UserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Path("/token_usage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Token-Usage", description = "Listing usage of Tokens", tags = {CLOUD_VISIBLE})
public class TokenUsageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(TokenUsageResource.class);
    private static final String DEFAULT_SORT_FIELD = AccessTokenEntity.FIELD_NAME;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_TOKEN_ID).title("Token ID").type(SearchQueryField.Type.OBJECT_ID).hidden(false).searchable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_USERNAME).title("Username").searchable(true).sortable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_USER_ID).title("User ID").hidden(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_TOKEN_NAME).title("Token Name").searchable(true).sortable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_CREATED_AT).title("Created").type(SearchQueryField.Type.DATE).searchable(true).sortable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_LAST_ACCESS).title("Last Accessed").type(SearchQueryField.Type.DATE).searchable(true).sortable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_USER_IS_EXTERNAL).title("External User")
                    .relatedCollection(UserImpl.COLLECTION_NAME).type(SearchQueryField.Type.BOOLEAN).sortable(true).filterable(true).build(),
            EntityAttribute.builder().id(TokenUsageDTO.FIELD_AUTH_BACKEND).title("Authentication Backend")
                    .relatedCollection(DBAuthServiceBackendService.COLLECTION_NAME).searchable(true).sortable(true).filterable(true).build()
    );
    static final EntityDefaults SETTINGS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT))))
            .build();

    private final TokenUsageService tokenUsageService;
    private final SearchQueryParser searchQueryParser;

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(AccessTokenEntity.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(AccessTokenEntity.FIELD_USERNAME, SearchQueryField.create(AccessTokenEntity.FIELD_USERNAME))
            .put(AccessTokenEntity.FIELD_NAME, SearchQueryField.create(AccessTokenEntity.FIELD_NAME))
            .build();

    @Inject
    public TokenUsageResource(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
        this.searchQueryParser = new SearchQueryParser(AccessTokenEntity.FIELD_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation(value = "Get paginated list of tokens")
    @RequiresPermissions(RestPermissions.TOKEN_USAGE_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<TokenUsageDTO> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                    @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                    @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                    @ApiParam(name = "sort",
                                                              value = "The field to sort the result on",
                                                              required = true,
                                                              allowableValues = "username,NAME")
                                                        @DefaultValue(AccessTokenEntity.FIELD_NAME) @QueryParam("sort") String sort,
                                                    @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                    @DefaultValue("asc") @QueryParam("order") SortOrder order) {
        LOG.debug("Incoming request to list token usages{}, on page {} with {} items per page.", query.isEmpty() ? "" : " matching " + query, page, perPage);
        final SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final PaginatedList<TokenUsageDTO> tokenUsages = tokenUsageService.loadTokenUsage(page, perPage, searchQuery, sort, order);
        LOG.debug("Found {} token usages for incoming request. Converting to response.", tokenUsages.size());
        final PaginatedList.PaginationInfo pagination = tokenUsages.pagination();
        return PageListResponse.create(query, pagination, pagination.total(), sort, order, tokenUsages, ATTRIBUTES, SETTINGS);
    }

}
