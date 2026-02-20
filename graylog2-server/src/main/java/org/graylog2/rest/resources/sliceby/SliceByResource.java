package org.graylog2.rest.resources.sliceby;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.grouping.EntityFieldBucketResponse;
import org.graylog2.database.grouping.EntityFieldGroupingService;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "SliceBy")
@Path("/slice_by")
@Produces(MediaType.APPLICATION_JSON)
public class SliceByResource extends RestResource {

    private final EntityFieldGroupingService entitySuggestionService;

    @Inject
    public SliceByResource(final EntityFieldGroupingService entitySuggestionService) {
        this.entitySuggestionService = entitySuggestionService;
    }

    @GET
    @Timed
    @Operation(summary = "Get slices for a given collection and field name")
    public EntityFieldBucketResponse getPage(@Parameter(name = "collection")
                                             @QueryParam("collection") String collection,
                                             @Parameter(name = "column")
                                             @QueryParam("column") @DefaultValue("title") String column,
                                             @Parameter(name = "query")
                                             @QueryParam("query") @DefaultValue("") String query,
                                             @Parameter(name = "resultsFilter")
                                             @QueryParam("resultsFilter") @DefaultValue("") String resultsFilter,
                                             @Parameter(name = "page")
                                             @QueryParam("page") @DefaultValue("1") int page,
                                             @Parameter(name = "per_page")
                                             @QueryParam("per_page") @DefaultValue("10") int perPage,
                                             @Parameter(name = "sort_order")
                                             @QueryParam("sort_order") @DefaultValue("DESC") EntityFieldGroupingService.SortOrder sortOrder,
                                             @Parameter(name = "sort_field")
                                             @QueryParam("sort_field") @DefaultValue("COUNT") EntityFieldGroupingService.SortField sortField) {

        return entitySuggestionService.groupByField(
                collection,
                column,
                query,
                resultsFilter,
                page,
                perPage,
                sortOrder,
                sortField,
                getSubject());

    }
}
