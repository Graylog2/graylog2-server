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
package org.graylog.plugins.pipelineprocessor.processors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstBaseListener;
import org.graylog.plugins.pipelineprocessor.ast.RuleAstWalker;
import org.graylog.plugins.pipelineprocessor.ast.Stage;
import org.graylog.plugins.pipelineprocessor.ast.expressions.EqualityExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.FunctionExpression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.functions.FromInput;
import org.graylog.plugins.pipelineprocessor.functions.messages.RouteToStream;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.InputService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.plugin.Message.FIELD_GL2_FORWARDER_INPUT;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;

public class PipelineAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineAnalyzer.class);

    public static final Set<String> REFERENCING_FUNCTIONS = Set.of(
            "from_forwarder_input",
            "from_input"
    );
    public static final Set<String> REFERENCING_VARIABLES = Set.of(
            FIELD_GL2_SOURCE_INPUT,
            FIELD_GL2_FORWARDER_INPUT
    );

    private final PipelineStreamConnectionsService connectionsService;
    private final InputService inputService;
    private final PipelineMetricRegistry pipelineMetricRegistry;
    private final StreamService streamService;

    @Inject
    public PipelineAnalyzer(
            PipelineStreamConnectionsService connectionsService,
            InputService inputService,
            MetricRegistry metricRegistry,
            StreamService streamService
    ) {
        this.connectionsService = connectionsService;
        this.inputService = inputService;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        this.streamService = streamService;
    }

    public Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> analyzePipelines(
            PipelineResolver resolver,
            List<PipelineRulesMetadataDao> ruleRecords) {
        final ImmutableMap<String, Pipeline> pipelines = resolver.resolvePipelines(pipelineMetricRegistry);
        return analyzePipelines(pipelines, ruleRecords);
    }

    public Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> analyzePipelines(
            ImmutableMap<String, Pipeline> pipelines,
            List<PipelineRulesMetadataDao> ruleRecords) {
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions = new HashMap<>();

        pipelines.values().forEach(pipeline -> {
            PipelineRulesMetadataDao.Builder rulesBuilder = PipelineRulesMetadataDao.builder().pipelineId(pipeline.id());
            Set<String> ruleSet = new HashSet<>();
            Set<String> functionSet = new HashSet<>();
            Set<String> deprecatedFunctionSet = new HashSet<>();
            Map<String, Set<String>> routingRulesMap = new HashMap<>();
            Map<String, String> routedStreamsMap = new HashMap<>();
            boolean hasInputReferences = false;

            Set<String> connectedStreams = connectionsService.loadByPipelineId(pipeline.id())
                    .stream()
                    .map(PipelineConnections::streamId)
                    .collect(Collectors.toSet());

            Set<Stage> stages = pipeline.stages();
            if (stages != null) {
                for (Stage stage : stages) {
                    List<Rule> stageRules = stage.getRules();
                    if (stageRules == null) continue;
                    for (Rule rule : stageRules) {
                        if (rule == null) continue;
                        ruleSet.add(rule.id());
                        boolean ruleHasReferences = analyzeRule(
                                pipeline, connectedStreams, rule, functionSet, deprecatedFunctionSet, routingRulesMap, routedStreamsMap, inputMentions);
                        hasInputReferences = hasInputReferences || ruleHasReferences;
                    }
                }
            }
            ruleRecords.add(rulesBuilder
                    .rules(ruleSet)
                    .streams(connectedStreams)
                    .functions(functionSet)
                    .deprecatedFunctions(deprecatedFunctionSet)
                    .streamsByRuleId(routingRulesMap)
                    .routedStreamTitleById(routedStreamsMap)
                    .hasInputReferences(hasInputReferences)
                    .build());
        });
        return inputMentions;
    }

    /**
     * Analyzes a rule for input references and function usage.
     *
     * @param pipeline               the pipeline the rule belongs to
     * @param connectedStreams       the streams connected to the pipeline
     * @param rule                   the rule to analyze
     * @param functions              return set of functions used in the rule
     * @param deprecatedFunctions    return set of deprecated functions used in the rule
     * @param routingRulesMap        return routing rules in the rule
     * @param routedStreamsMap       return stream routing targets in the rule
     * @param inputMentions          map to collect input mentions
     * @return true if the rule references any inputs, false otherwise
     */
    private boolean analyzeRule(Pipeline pipeline, Set<String> connectedStreams, Rule rule,
                                Set<String> functions, Set<String> deprecatedFunctions,
                                Map<String, Set<String>> routingRulesMap, Map<String, String> routedStreamsMap,
                                Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions) {
        MetaDataListener ruleListener = new MetaDataListener(pipeline, connectedStreams, rule, inputMentions);
        try {
            new RuleAstWalker().walk(ruleListener, rule);
        } catch (Exception e) {
            LOG.warn("Pipeline metadata analysis failed for rule [{}] in pipeline [{}], likely due to invalid rule syntax. Skipping rule...",
                    rule.name(), pipeline.name(), e);
            return false;
        }
        functions.addAll(ruleListener.getFunctions());
        deprecatedFunctions.addAll(ruleListener.getDeprecatedFunctions());
        routingRulesMap.putAll(ruleListener.getRoutingRules());
        routedStreamsMap.putAll(ruleListener.getRoutedStreams());
        return ruleListener.hasInputReference();
    }

    protected boolean isValidInputId(String inputId) {
        try {
            return (inputService.find(inputId) != null);
        } catch (NotFoundException e) {
            return false;
        }
    }

    class MetaDataListener extends RuleAstBaseListener {
        private final Set<String> functions = new HashSet<>();
        private final Set<String> deprecatedFunctions = new HashSet<>();
        private final Map<String, Set<String>> routingRulesMap = new HashMap<>();
        private final Map<String, String> routedStreamsMap = new HashMap<>();
        private boolean hasInputReference = false;
        private final Pipeline pipeline;
        private final Set<String> connectedStreams;
        private final Rule rule;
        private final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions;

        MetaDataListener(Pipeline pipeline, Set<String> connectedStreams, Rule rule,
                         Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions) {
            this.pipeline = pipeline;
            this.connectedStreams = connectedStreams;
            this.rule = rule;
            this.inputMentions = inputMentions;
        }

        @Override
        public void enterEquality(EqualityExpression expr) {
            Expression inputIdCandidate = null;
            if (isReferenced(expr.left().toString())) {
                inputIdCandidate = expr.right();
            } else if (isReferenced(expr.right().toString())) {
                inputIdCandidate = expr.left();
            }
            if (inputIdCandidate instanceof StringExpression stringExpression) {
                final String inputId = (String) stringExpression.evaluateUnsafe(null);
                if (isValidInputId(inputId)) {
                    createOrUpdateMention(inputId);
                }
            }
        }

        private boolean isReferenced(String value) {
            return REFERENCING_VARIABLES.stream().anyMatch(value::contains);
        }

        @Override
        public void enterFunctionCall(FunctionExpression expr) {
            final FunctionDescriptor<?> descriptor = expr.getFunction().descriptor();
            analyzeFunctions(descriptor);
            analyzeRouting(descriptor, expr.getArgs());
            analyzeInputs(descriptor, expr.getArgs());
        }

        private void analyzeFunctions(FunctionDescriptor<?> descriptor) {
            functions.add(descriptor.name());
            if (Boolean.TRUE.equals(descriptor.deprecated())) {
                deprecatedFunctions.add(descriptor.name());
            }
        }

        private void analyzeRouting(FunctionDescriptor<?> descriptor, FunctionArgs args) {
            if (descriptor.name().equals(RouteToStream.NAME)) {
                if (args.getPreComputedValue(RouteToStream.ID_ARG) != null) {
                    String streamId = args.getPreComputedValue(RouteToStream.ID_ARG).toString();
                    String streamTitle = streamService.streamTitleFromCache(streamId);
                    if (streamTitle != null) {
                        routedStreamsMap.put(streamId, streamTitle);
                        addToRoutingRules(rule.id(), streamId);
                    } else {
                        LOG.warn("Could not find stream with id '{}'", streamId);
                    }
                } else if (args.getPreComputedValue(RouteToStream.NAME_ARG) != null) {
                    String title = args.getPreComputedValue(RouteToStream.NAME_ARG).toString();
                    List<Stream> streams = streamService.loadAllByTitle(title);
                    if (streams.isEmpty()) {
                        LOG.warn("Could not find stream with title '{}'", title);
                    } else {
                        streams.forEach(stream -> {
                            routedStreamsMap.put(stream.getId(), stream.getTitle());
                            addToRoutingRules(rule.id(), stream.getId());
                        });
                    }
                }
            }
        }

        private void analyzeInputs(FunctionDescriptor<?> descriptor, FunctionArgs args) {
            if (REFERENCING_FUNCTIONS.contains(descriptor.name())) {
                String inputId = null;
                if (args.getPreComputedValue(FromInput.ID_ARG) != null) {
                    inputId = args.getPreComputedValue(FromInput.ID_ARG).toString();
                } else if (args.getPreComputedValue(FromInput.NAME_ARG) != null) {
                    inputId = getInputId(args.getPreComputedValue(FromInput.NAME_ARG).toString());
                }
                if (inputId != null) {
                    createOrUpdateMention(inputId);
                }
            }
        }

        private String getInputId(String inputName) {
            final List<String> inputIds = inputService.findIdsByTitle(inputName);
            if (inputIds.isEmpty()) {
                LOG.warn("Could not find input with name '{}'", inputName);
                return null;
            }
            if (inputIds.size() > 1) {
                LOG.warn("Multiple inputs found with name '{}', using the first one with id '{}'", inputName, inputIds.getFirst());
            }
            return inputIds.getFirst();
        }

        private void createOrUpdateMention(String inputId) {
            hasInputReference = true;
            if (!inputMentions.containsKey(inputId)) {
                // first mention of this input
                final Set<PipelineInputsMetadataDao.MentionedInEntry> mentionedInEntries = new HashSet<>();
                mentionedInEntries.add(new PipelineInputsMetadataDao.MentionedInEntry(
                        pipeline.id(), rule.id(), connectedStreams));
                inputMentions.put(inputId, mentionedInEntries);
            } else {
                // update existing mention
                final Set<PipelineInputsMetadataDao.MentionedInEntry> mentionedInEntries = inputMentions.get(inputId);
                final Optional<PipelineInputsMetadataDao.MentionedInEntry> optMention = mentionedInEntries.stream()
                        .filter(e -> e.pipelineId().equals(pipeline.id()) && e.ruleId().equals(rule.id()))
                        .findFirst();
                if (optMention.isPresent()) {
                    optMention.get().connectedStreams().addAll(connectedStreams);
                } else {
                    mentionedInEntries.add(new PipelineInputsMetadataDao.MentionedInEntry(
                            pipeline.id(), rule.id(), connectedStreams));
                }
            }
        }

        public Set<String> getFunctions() {
            return functions;
        }

        public Set<String> getDeprecatedFunctions() {
            return deprecatedFunctions;
        }

        public Map<String, Set<String>> getRoutingRules() {
            return routingRulesMap;
        }

        private void addToRoutingRules(String ruleId, String streamId) {
            routingRulesMap.putIfAbsent(ruleId, new HashSet<>());
            routingRulesMap.get(ruleId).add(streamId);
        }

        public Map<String, String> getRoutedStreams() {
            return routedStreamsMap;
        }

        public boolean hasInputReference() {
            return hasInputReference;
        }
    }

}
