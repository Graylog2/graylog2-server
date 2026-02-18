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
import org.graylog2.database.grouping.EntityFieldGroupingService;
import org.graylog2.database.grouping.SliceByResponse;
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
    @Operation(summary = "Get a paginated list of suggested entities")
    public SliceByResponse getPage(@Parameter(name = "collection")
                                   @QueryParam("collection") String collection,
                                   @Parameter(name = "column")
                                   @QueryParam("column") @DefaultValue("title") String column,
                                   @Parameter(name = "page")
                                   @QueryParam("page") @DefaultValue("1") int page,
                                   @Parameter(name = "per_page")
                                   @QueryParam("per_page") @DefaultValue("10") int perPage,
                                   @Parameter(name = "query")
                                   @QueryParam("query") @DefaultValue("") String query) {

        return new SliceByResponse(
                entitySuggestionService.groupByField(collection, column, query, "TBD", getSubject()),
                null
        );
    }
}
