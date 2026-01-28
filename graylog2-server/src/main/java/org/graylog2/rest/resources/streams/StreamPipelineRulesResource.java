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
    import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
    import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
    import org.graylog.plugins.pipelineprocessor.db.PipelineService;
    import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
    import org.graylog.plugins.pipelineprocessor.db.RuleDao;
    import org.graylog.plugins.pipelineprocessor.db.RuleService;
    import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
    import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
    import org.graylog2.database.NotFoundException;
    import org.graylog2.database.PaginatedList;
    import org.graylog2.database.pagination.EntityPaginationHelper;
    import org.graylog2.rest.models.SortOrder;
    import org.graylog2.rest.models.tools.responses.PageListResponse;
    import org.graylog2.rest.resources.entities.EntityAttribute;
    import org.graylog2.rest.resources.entities.EntityDefaults;
    import org.graylog2.rest.resources.entities.Sorting;
    import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
    import org.graylog2.rest.resources.streams.responses.StreamReference;
    import org.graylog2.search.SearchQueryField;
    import org.graylog2.shared.rest.PublicCloudAPI;
    import org.graylog2.shared.rest.resources.RestResource;
    import org.graylog2.streams.StreamService;

    import java.util.List;
    import java.util.Locale;
    import java.util.Map;
    import java.util.Objects;
    import java.util.function.Function;
    import java.util.stream.Stream;

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
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).searchable(false).hidden(true).build(),
            EntityAttribute.builder().id("rule_id").title("Pipeline Rule ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE_RULE).title("Pipeline Rule").searchable(true).build(),
            EntityAttribute.builder().id("pipeline_id").title("Pipeline ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE).title("Pipeline").searchable(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_CONNECTED_STREAM).title("Connected Streams").searchable(true).build()
    );
        private static final Map<String, Function<StreamPipelineRulesResponse, String>> FIELD_EXTRACTORS = Map.of(
                ATTRIBUTE_PIPELINE_RULE, StreamPipelineRulesResponse::ruleName,
                ATTRIBUTE_PIPELINE, StreamPipelineRulesResponse::pipelineTitle,
                ATTRIBUTE_CONNECTED_STREAM, StreamPipelineRulesResponse::connectedStreamTitles
        );
        public static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    private final MongoDbPipelineMetadataService mongoDbPipelineMetadataService;
    private final PipelineService pipelineService;
    private final RuleService ruleService;
    private final PipelineStreamConnectionsService connectionsService;
    private final StreamService streamService;

    @Inject
    public StreamPipelineRulesResource(MongoDbPipelineMetadataService mongoDbPipelineMetadataService,
                                       PipelineService pipelineService,
                                       RuleService ruleService,
                                       PipelineStreamConnectionsService connectionsService,
                                       StreamService streamService) {
        this.mongoDbPipelineMetadataService = mongoDbPipelineMetadataService;
        this.pipelineService = pipelineService;
        this.ruleService = ruleService;
        this.connectionsService = connectionsService;
        this.streamService = streamService;
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
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       required = true,
                       schema = @Schema(allowableValues = {"rule", "pipeline", "connected_streams"}))
            @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order) {

        // Pagination is primarily for UX purposes - OK to fetch all and then paginate in memory
        List<StreamPipelineRulesResponse> responseList =
                mongoDbPipelineMetadataService.getRoutingPipelines(streamId).stream()
                        .flatMap(dao -> buildResponse(dao, streamId))
                        .filter(EntityPaginationHelper.buildPredicate(query, FIELD_EXTRACTORS))
                        .filter(EntityPaginationHelper.entityFiltersPredicate(filters,
                                filter -> EntityPaginationHelper.buildPredicate(filter, FIELD_EXTRACTORS)))
                        .sorted(EntityPaginationHelper.buildComparator(sort, order, FIELD_EXTRACTORS))
                        .toList();

        final PaginatedList<StreamPipelineRulesResponse> paginatedList =
                new PaginatedList<>(responseList, responseList.size(), page, perPage);

        return PageListResponse.create(
                query, paginatedList, sort, order.toString(), attributes, DEFAULTS);
    }


    private Stream<StreamPipelineRulesResponse> buildResponse(PipelineRulesMetadataDao dao, String streamId) {
        List<StreamPipelineRulesResponse> responseList = new java.util.ArrayList<>();
        final List<String> relevantRules = dao.streamsByRuleId().keySet().stream()
                .filter(ruleId -> dao.streamsByRuleId().get(ruleId).contains(streamId)).toList();

        PipelineDao pipelineDao;
        try {
            pipelineDao = pipelineService.load(dao.pipelineId());
        } catch (NotFoundException e) {
            return Stream.empty();
        }

        List<StreamReference> connectedStreams = connectionsService.loadByPipelineId(pipelineDao.id()).stream()
                .map(PipelineConnections::streamId)
                .map(id -> {
                    try {
                        return new StreamReference(id, streamService.load(id).getTitle());
                    } catch (NotFoundException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        relevantRules.forEach(ruleId -> {
            try {
                RuleDao ruleDao = ruleService.load(ruleId);
                responseList.add(
                        new StreamPipelineRulesResponse(
                                ruleId,
                                dao.pipelineId(),
                                pipelineDao.title(),
                                ruleId,
                                ruleDao.title(),
                                connectedStreams));
            } catch (NotFoundException e) {
                // Skip pipelines or rules that no longer exist
            }
        });
        return responseList.stream();
    }
}
