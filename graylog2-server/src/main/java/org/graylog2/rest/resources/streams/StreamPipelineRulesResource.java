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
package org.graylog2.rest.resources.streams;

    import com.codahale.metrics.annotation.Timed;
    import io.swagger.v3.oas.annotations.Operation;
    import io.swagger.v3.oas.annotations.Parameter;
    import io.swagger.v3.oas.annotations.media.Schema;
    import io.swagger.v3.oas.annotations.tags.Tag;
    import jakarta.inject.Inject;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.ws.rs.DefaultValue;
    import jakarta.ws.rs.GET;
    import jakarta.ws.rs.Path;
    import jakarta.ws.rs.PathParam;
    import jakarta.ws.rs.Produces;
    import jakarta.ws.rs.QueryParam;
    import jakarta.ws.rs.core.MediaType;
    import org.apache.shiro.authz.annotation.RequiresAuthentication;
    import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
    import org.graylog2.database.PaginatedList;
    import org.graylog2.rest.models.SortOrder;
    import org.graylog2.rest.models.tools.responses.PageListResponse;
    import org.graylog2.rest.resources.entities.EntityAttribute;
    import org.graylog2.rest.resources.entities.EntityDefaults;
    import org.graylog2.rest.resources.entities.Sorting;
    import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
    import org.graylog2.search.SearchQueryField;
    import org.graylog2.shared.rest.PublicCloudAPI;
    import org.graylog2.shared.rest.resources.RestResource;

    import java.util.List;
    import java.util.Locale;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "Stream/RoutingRules", description = "Stream routing with pipeline rules")
@Path("/routing_rules")
public class StreamPipelineRulesResource extends RestResource {
    private static final String ATTRIBUTE_PIPELINE_RULE = "rule";
    private static final String ATTRIBUTE_PIPELINE = "pipeline";
    private static final String ATTRIBUTE_CONNECTED_STREAM = "connected_streams";
    private static final String DEFAULT_SORT_FIELD = ATTRIBUTE_PIPELINE_RULE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> attributes = List.of(
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(false).build(),
            EntityAttribute.builder().id("rule_id").title("Pipeline Rule ID").hidden(true).searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE_RULE).title("Pipeline Rule")
                    .searchable(true).sortable(true).filterable(true)
                    .relatedCollection("pipeline_processor_rules").relatedIdentifier("_id").relatedProperty("title")
                    .build(),
            EntityAttribute.builder().id("pipeline_id").title("Pipeline ID").hidden(true).searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE).title("Source pipeline")
                    .searchable(true).sortable(true).filterable(true)
                    .relatedCollection("pipeline_processor_pipelines").relatedIdentifier("_id").relatedProperty("title")
                    .build(),
            EntityAttribute.builder().id(ATTRIBUTE_CONNECTED_STREAM).title("Source streams").searchable(false).sortable(false).build()
    );
    private static final EntityDefaults settings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    private final MongoDbPipelineMetadataService mongoDbPipelineMetadataService;

    @Inject
    public StreamPipelineRulesResource(MongoDbPipelineMetadataService mongoDbPipelineMetadataService) {
        this.mongoDbPipelineMetadataService = mongoDbPipelineMetadataService;
    }

    @GET
    @Timed
    @Path("/paginated/{streamId}")
    @Operation(summary = "Get a paginated list of associated pipeline rules for the specified stream")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<StreamPipelineRulesResponse> getPage(
            @Parameter(name = "streamId", required = true) @PathParam("streamId") @NotBlank String streamId,
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters, // currently unused
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       required = true,
                       schema = @Schema(allowableValues = {"rule", "pipeline", "connected_streams"}))
            @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order) {

        final PaginatedList<StreamPipelineRulesResponse> result =
                mongoDbPipelineMetadataService.getRoutingRulesPaginated(
                        streamId, query, sort, order, page, perPage);

        return PageListResponse.create(
                query, result.pagination(),
                result.grandTotal().orElse(0L), sort, order, result.delegate(), attributes, settings);
    }
}
