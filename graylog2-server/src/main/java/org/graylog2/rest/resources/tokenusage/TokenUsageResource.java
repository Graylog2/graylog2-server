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
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tokenusage.TokenUsage;
import org.graylog2.rest.models.tokenusage.TokenUsageDTO;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.tokenusage.TokenUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Path("/token_usage")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Token-Usage", description = "Listing usage of Tokens", tags = {CLOUD_VISIBLE})
public class TokenUsageResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(TokenUsageResource.class);

    private final TokenUsageService tokenUsageService;
    private final SearchQueryParser searchQueryParser;

    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(TokenUsageDTO.FIELD_ID, SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put(TokenUsageDTO.FIELD_USERNAME, SearchQueryField.create(TokenUsageDTO.FIELD_USERNAME))
            .put(TokenUsageDTO.FIELD_NAME, SearchQueryField.create(TokenUsageDTO.FIELD_NAME))
            .build();

    @Inject
    public TokenUsageResource(TokenUsageService tokenUsageService) {
        this.tokenUsageService = tokenUsageService;
        this.searchQueryParser = new SearchQueryParser(TokenUsageDTO.FIELD_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation(value = "Get paginated list of tokens")
    @RequiresPermissions(RestPermissions.TOKEN_USAGE_READ)
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<TokenUsage> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                 @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                 @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                 @ApiParam(name = "sort",
                                                           value = "The field to sort the result on",
                                                           required = true,
                                                           allowableValues = "title,description")
                                                 @DefaultValue(TokenUsageDTO.FIELD_NAME) @QueryParam("sort") String sort,
                                                 @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                 @DefaultValue("asc") @QueryParam("order") SortOrder order) {
        LOG.debug("Incoming request to list token usages{}, on page {} with {} items per page.", query.isEmpty() ? "" : " matching " + query, page, perPage);
        final SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final PaginatedList<TokenUsage> tokenUsages = tokenUsageService.loadTokenUsage(page, perPage, searchQuery, sort, order);
        LOG.debug("Found {} token usages for incoming request. Converting to response.", tokenUsages.size());
        return PaginatedResponse.create("token_usage", tokenUsages, query);
    }

}
