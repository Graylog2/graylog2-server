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
package org.graylog2.inputs.diagnosis;

import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
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
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class InputRoutingRulesService {

    // Pipeline rules endpoint attributes
    private static final String ATTRIBUTE_PIPELINE_RULE = "rule";
    private static final String ATTRIBUTE_PIPELINE = "pipeline";
    private static final String ATTRIBUTE_CONNECTED_STREAMS = "connected_streams";
    private static final String DEFAULT_PIPELINE_SORT_FIELD = ATTRIBUTE_PIPELINE_RULE;
    private static final String DEFAULT_SORT_DIRECTION = "asc";
    static final List<EntityAttribute> PIPELINE_ATTRIBUTES = List.of(
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id("rule_id").title("Pipeline Rule ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE_RULE).title("Pipeline Rule").searchable(false).build(),
            EntityAttribute.builder().id("pipeline_id").title("Pipeline ID").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_PIPELINE).title("Source pipeline").searchable(false).build(),
            EntityAttribute.builder().id("stage").title("Stage").searchable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_CONNECTED_STREAMS).title("Connected streams").searchable(false).build()
    );
    static final EntityDefaults PIPELINE_SETTINGS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_PIPELINE_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    // Stream rules endpoint attributes
    private static final String ATTRIBUTE_STREAM = "stream";
    private static final String ATTRIBUTE_RULE_FIELD = "rule_field";
    private static final String ATTRIBUTE_RULE_TYPE = "rule_type";
    private static final String ATTRIBUTE_RULE_VALUE = "rule_value";
    private static final String DEFAULT_STREAM_SORT_FIELD = ATTRIBUTE_STREAM;
    static final List<EntityAttribute> STREAM_RULE_ATTRIBUTES = List.of(
            EntityAttribute.builder().id("id").title("id").type(SearchQueryField.Type.OBJECT_ID).hidden(true).searchable(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_STREAM).title("Stream").searchable(false).build(),
            EntityAttribute.builder().id("rule").title("Rule").searchable(false).sortable(false).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_FIELD).title("Rule field").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_TYPE).title("Rule type").searchable(false).hidden(true).build(),
            EntityAttribute.builder().id(ATTRIBUTE_RULE_VALUE).title("Rule value").searchable(false).hidden(true).build()
    );
    static final EntityDefaults STREAM_RULE_SETTINGS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_STREAM_SORT_FIELD, Sorting.Direction.valueOf(DEFAULT_SORT_DIRECTION.toUpperCase(Locale.ROOT)))).build();

    private final MongoDbInputsMetadataService metadataService;
    private final PipelineService pipelineService;
    private final PipelineRuleParser pipelineRuleParser;
    private final RuleService ruleService;
    private final StreamRuleService streamRuleService;
    private final StreamService streamService;

    @Inject
    public InputRoutingRulesService(MongoDbInputsMetadataService metadataService,
                                    PipelineService pipelineService,
                                    PipelineRuleParser pipelineRuleParser,
                                    RuleService ruleService,
                                    StreamRuleService streamRuleService,
                                    StreamService streamService) {
        this.metadataService = metadataService;
        this.pipelineService = pipelineService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.ruleService = ruleService;
        this.streamRuleService = streamRuleService;
        this.streamService = streamService;
    }

    public PageListResponse<StreamPipelineRulesResponse> getPipelineRulesPage(
            String inputId,
            Predicate<String> pipelinePermissionCheck,
            Predicate<String> streamPermissionCheck,
            int page, int perPage, String query, String sort, SortOrder order) {

        PipelineInputsMetadataDao metadataDao;
        try {
            metadataDao = metadataService.getByInputId(inputId);
        } catch (NotFoundException e) {
            final PaginatedList<StreamPipelineRulesResponse> emptyList = PaginatedList.emptyList(page, perPage);
            return PageListResponse.create(query, emptyList.pagination(),
                    emptyList.grandTotal().orElse(0L), sort, order, emptyList.delegate(), PIPELINE_ATTRIBUTES, PIPELINE_SETTINGS);
        }

        List<StreamPipelineRulesResponse> allResults = new ArrayList<>();
        for (PipelineInputsMetadataDao.MentionedInEntry entry : metadataDao.mentionedIn()) {
            if (!pipelinePermissionCheck.test(entry.pipelineId())) {
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

            int stageNumber = -1;
            try {
                Pipeline parsedPipeline = pipelineRuleParser.parsePipeline(pipelineDao.id(), pipelineDao.source());
                for (Stage stage : parsedPipeline.stages()) {
                    if (stage.ruleReferences().contains(ruleDao.title())) {
                        stageNumber = stage.stage() + 1;
                        break;
                    }
                }
            } catch (Exception e) {
                // Parse failure — leave stage as -1
            }

            Set<String> connectedStreamIds = entry.connectedStreams();
            List<StreamReference> connectedStreams = connectedStreamIds == null ? List.of() : connectedStreamIds.stream()
                    .filter(streamPermissionCheck)
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
                    stageNumber,
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

        // Pagination
        int total = allResults.size();
        int fromIndex = Math.min((page - 1) * perPage, total);
        int toIndex = Math.min(fromIndex + perPage, total);
        List<StreamPipelineRulesResponse> pageSlice = allResults.subList(fromIndex, toIndex);

        final PaginatedList<StreamPipelineRulesResponse> paginatedList =
                new PaginatedList<>(pageSlice, total, page, perPage);

        return PageListResponse.create(query, paginatedList.pagination(),
                paginatedList.grandTotal().orElse(0L), sort, order, paginatedList.delegate(), PIPELINE_ATTRIBUTES, PIPELINE_SETTINGS);
    }

    public PageListResponse<InputStreamRulesResponse> getStreamRulesPage(
            String inputId,
            Predicate<String> streamPermissionCheck,
            int page, int perPage, String query, String sort, SortOrder order) {

        List<StreamRule> streamRules = streamRuleService.loadForInput(inputId);

        List<InputStreamRulesResponse> allResults = new ArrayList<>();
        for (StreamRule streamRule : streamRules) {
            if (!streamPermissionCheck.test(streamRule.getStreamId())) {
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

        // Pagination
        int total = allResults.size();
        int fromIndex = Math.min((page - 1) * perPage, total);
        int toIndex = Math.min(fromIndex + perPage, total);
        List<InputStreamRulesResponse> pageSlice = allResults.subList(fromIndex, toIndex);

        final PaginatedList<InputStreamRulesResponse> paginatedList =
                new PaginatedList<>(pageSlice, total, page, perPage);

        return PageListResponse.create(query, paginatedList.pagination(),
                paginatedList.grandTotal().orElse(0L), sort, order, paginatedList.delegate(), STREAM_RULE_ATTRIBUTES, STREAM_RULE_SETTINGS);
    }

    private Comparator<StreamPipelineRulesResponse> getPipelineRulesComparator(String sort) {
        return switch (sort) {
            case ATTRIBUTE_PIPELINE -> Comparator.comparing(StreamPipelineRulesResponse::pipeline,
                    String.CASE_INSENSITIVE_ORDER);
            case "stage" -> Comparator.comparingInt(StreamPipelineRulesResponse::stage);
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
