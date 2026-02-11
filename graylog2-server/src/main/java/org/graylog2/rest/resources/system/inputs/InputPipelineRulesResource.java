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
package org.graylog2.rest.resources.system.inputs;

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
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.rest.PipelineRestPermissions;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.rest.resources.streams.responses.StreamPipelineRulesResponse;
import org.graylog2.rest.resources.streams.responses.StreamReference;
import org.graylog2.rest.resources.system.inputs.responses.InputStreamRulesResponse;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@RequiresAuthentication
@PublicCloudAPI
@Tag(name = "System/Inputs/RoutingRules", description = "Input routing with pipeline and stream rules")
@Path("/system/inputs/routing_rules")
public class InputPipelineRulesResource extends RestResource {

    // Pipeline rules endpoint attributes
    private static final String ATTRIBUTE_PIPELINE_RULE = "rule";
    private static final String ATTRIBUTE_PIPELINE = "pipeline";
    private static final String ATTRIBUTE_CONNECTED_STREAMS = "connected_streams";
    private static final String DEFAULT_PIPELINE_SORT_FIELD = ATTRIBUTE_PIPELINE_RULE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    private static final List<EntityAttribute> pipelineAttributes = List.of(
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id("rule_id").title("Pipeline Rule ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE_RULE).title("Pipeline Rule").searchable(false).build(),
            EntityAttribute.builder().id("pipeline_id").title("Pipeline ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE).title("Source pipeline").searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_CONNECTED_STREAMS).title("Connected streams").searchable(false).build()
    );
    private static final EntityDefaults pipelineSettings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_PIPELINE_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    // Stream rules endpoint attributes
    private static final String ATTRIBUTE_STREAM = "stream";
    private static final String ATTRIBUTE_RULE_FIELD = "rule_field";
    private static final String ATTRIBUTE_RULE_TYPE = "rule_type";
    private static final String ATTRIBUTE_RULE_VALUE = "rule_value";
    private static final String DEFAULT_STREAM_SORT_FIELD = ATTRIBUTE_STREAM;
    private static final List<EntityAttribute> streamRuleAttributes = List.of(
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_STREAM).title("Stream").searchable(false).build(),
            EntityAttribute.builder().id("rule").title("Rule").searchable(false).sortable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_FIELD).title("Rule field").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_TYPE).title("Rule type").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_VALUE).title("Rule value").searchable(false).hidden(true).build()
    );
    private static final EntityDefaults streamRuleSettings = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_STREAM_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    private final MongoDbInputsMetadataService metadataService;
    private final PipelineService pipelineService;
    private final RuleService ruleService;
    private final StreamRuleService streamRuleService;
    private final StreamService streamService;

    @Inject
    public InputPipelineRulesResource(MongoDbInputsMetadataService metadataService,
                                      PipelineService pipelineService,
                                      RuleService ruleService,
                                      StreamRuleService streamRuleService,
                                      StreamService streamService) {
        this.metadataService = metadataService;
        this.pipelineService = pipelineService;
        this.ruleService = ruleService;
        this.streamRuleService = streamRuleService;
        this.streamService = streamService;
    }

    @GET
    @Timed
    @Path("/pipeline_rules/{inputId}")
    @Operation(summary = "Get a paginated list of pipeline rules that reference the specified input")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<StreamPipelineRulesResponse> getPipelineRulesPage(
            @Parameter(name = "inputId", required = true) @PathParam("inputId") @NotBlank String inputId,
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       required = true,
                       schema = @Schema(allowableValues = {"rule", "pipeline", "connected_streams"}))
            @DefaultValue(DEFAULT_PIPELINE_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order) {

        checkPermission(RestPermissions.INPUTS_READ, inputId);

        PipelineInputsMetadataDao metadataDao;
        try {
            metadataDao = metadataService.getByInputId(inputId);
        } catch (NotFoundException e) {
            // Input with no pipeline mentions is valid - return empty page
            final PaginatedList<StreamPipelineRulesResponse> emptyList = PaginatedList.emptyList(page, perPage);
            return PageListResponse.create(query, emptyList.pagination(),
                    emptyList.grandTotal().orElse(0L), sort, order, emptyList.delegate(), pipelineAttributes, pipelineSettings);
        }

        List<StreamPipelineRulesResponse> allResults = new ArrayList<>();
        for (PipelineInputsMetadataDao.MentionedInEntry entry : metadataDao.mentionedIn()) {
            if (!isPermitted(PipelineRestPermissions.PIPELINE_READ, entry.pipelineId())) {
                continue;
            }

            PipelineDao pipelineDao;
            try {
                pipelineDao = pipelineService.load(entry.pipelineId());
            } catch (NotFoundException e) {
                continue;
            }

            RuleDao ruleDao;
            try {
                ruleDao = ruleService.load(entry.ruleId());
            } catch (NotFoundException e) {
                continue;
            }

            Set<String> connectedStreamIds = entry.connectedStreams();
            List<StreamReference> connectedStreams = connectedStreamIds == null ? List.of() : connectedStreamIds.stream()
                    .filter(streamId -> isPermitted(RestPermissions.STREAMS_READ, streamId))
                    .map(streamId -> {
                        try {
                            return new StreamReference(streamId, streamService.load(streamId).getTitle());
                        } catch (NotFoundException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();

            allResults.add(new StreamPipelineRulesResponse(
                    entry.ruleId(),
                    entry.pipelineId(),
                    pipelineDao.title(),
                    entry.ruleId(),
                    ruleDao.title(),
                    connectedStreams));
        }

        // Apply query filter (case-insensitive match on rule/pipeline titles)
        if (query != null && !query.isBlank()) {
            final String lowerQuery = query.toLowerCase(Locale.ROOT);
            allResults = allResults.stream()
                    .filter(r -> r.rule().toLowerCase(Locale.ROOT).contains(lowerQuery)
                            || r.pipeline().toLowerCase(Locale.ROOT).contains(lowerQuery))
                    .toList();
        }

        // Sort
        Comparator<StreamPipelineRulesResponse> comparator = getPipelineRulesComparator(sort);
        if (order == SortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }
        allResults = allResults.stream().sorted(comparator).toList();

        // Real pagination: slice for the requested page
        int total = allResults.size();
        int fromIndex = Math.min((page - 1) * perPage, total);
        int toIndex = Math.min(fromIndex + perPage, total);
        List<StreamPipelineRulesResponse> pageSlice = allResults.subList(fromIndex, toIndex);

        final PaginatedList<StreamPipelineRulesResponse> paginatedList =
                new PaginatedList<>(pageSlice, total, page, perPage);

        return PageListResponse.create(query, paginatedList.pagination(),
                paginatedList.grandTotal().orElse(0L), sort, order, paginatedList.delegate(), pipelineAttributes, pipelineSettings);
    }

    private Comparator<StreamPipelineRulesResponse> getPipelineRulesComparator(String sort) {
        return switch (sort) {
            case ATTRIBUTE_PIPELINE -> Comparator.comparing(StreamPipelineRulesResponse::pipeline,
                    String.CASE_INSENSITIVE_ORDER);
            case ATTRIBUTE_CONNECTED_STREAMS -> Comparator.comparing(
                    r -> r.connectedStreams().stream()
                            .map(StreamReference::title)
                            .sorted()
                            .findFirst()
                            .orElse(""),
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(StreamPipelineRulesResponse::rule, String.CASE_INSENSITIVE_ORDER);
        };
    }

    @GET
    @Timed
    @Path("/stream_rules/{inputId}")
    @Operation(summary = "Get a paginated list of stream rules that reference the specified input")
    @Produces(MediaType.APPLICATION_JSON)
    public PageListResponse<InputStreamRulesResponse> getStreamRulesPage(
            @Parameter(name = "inputId", required = true) @PathParam("inputId") @NotBlank String inputId,
            @Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
            @Parameter(name = "sort",
                       description = "The field to sort the result on",
                       required = true,
                       schema = @Schema(allowableValues = {"stream", "rule_field", "rule_type", "rule_value"}))
            @DefaultValue(DEFAULT_STREAM_SORT_FIELD) @QueryParam("sort") String sort,
            @Parameter(name = "order", description = "The sort direction",
                       schema = @Schema(allowableValues = {"asc", "desc"}))
            @DefaultValue(DEFAULT_SORT_DIRECTION) @QueryParam("order") SortOrder order) {

        checkPermission(RestPermissions.INPUTS_READ, inputId);

        List<StreamRule> streamRules = streamRuleService.loadForInput(inputId);

        List<InputStreamRulesResponse> allResults = new ArrayList<>();
        for (StreamRule streamRule : streamRules) {
            if (!isPermitted(RestPermissions.STREAMS_READ, streamRule.getStreamId())) {
                continue;
            }

            String streamTitle = streamService.streamTitleFromCache(streamRule.getStreamId());

            allResults.add(new InputStreamRulesResponse(
                    streamRule.getId(),
                    streamRule.getStreamId(),
                    streamTitle,
                    streamRule.getField(),
                    streamRule.getType().toInteger(),
                    streamRule.getValue(),
                    streamRule.getInverted(),
                    streamRule.getDescription()));
        }

        // Apply query filter (case-insensitive match on stream title, rule field, rule value)
        if (query != null && !query.isBlank()) {
            final String lowerQuery = query.toLowerCase(Locale.ROOT);
            allResults = allResults.stream()
                    .filter(r -> (r.stream() != null && r.stream().toLowerCase(Locale.ROOT).contains(lowerQuery))
                            || (r.ruleField() != null && r.ruleField().toLowerCase(Locale.ROOT).contains(lowerQuery))
                            || (r.ruleValue() != null && r.ruleValue().toLowerCase(Locale.ROOT).contains(lowerQuery)))
                    .toList();
        }

        // Sort
        Comparator<InputStreamRulesResponse> comparator = getStreamRulesComparator(sort);
        if (order == SortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }
        allResults = allResults.stream().sorted(comparator).toList();

        // Real pagination: slice for the requested page
        int total = allResults.size();
        int fromIndex = Math.min((page - 1) * perPage, total);
        int toIndex = Math.min(fromIndex + perPage, total);
        List<InputStreamRulesResponse> pageSlice = allResults.subList(fromIndex, toIndex);

        final PaginatedList<InputStreamRulesResponse> paginatedList =
                new PaginatedList<>(pageSlice, total, page, perPage);

        return PageListResponse.create(query, paginatedList.pagination(),
                paginatedList.grandTotal().orElse(0L), sort, order, paginatedList.delegate(), streamRuleAttributes, streamRuleSettings);
    }

    private Comparator<InputStreamRulesResponse> getStreamRulesComparator(String sort) {
        return switch (sort) {
            case ATTRIBUTE_RULE_FIELD -> Comparator.comparing(
                    r -> r.ruleField() != null ? r.ruleField() : "",
                    String.CASE_INSENSITIVE_ORDER);
            case ATTRIBUTE_RULE_TYPE -> Comparator.comparingInt(InputStreamRulesResponse::ruleType);
            case ATTRIBUTE_RULE_VALUE -> Comparator.comparing(
                    r -> r.ruleValue() != null ? r.ruleValue() : "",
                    String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(
                    r -> r.stream() != null ? r.stream() : "",
                    String.CASE_INSENSITIVE_ORDER);
        };
    }
}
