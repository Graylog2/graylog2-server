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
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.inputs.InputRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.functions.FromInput.ID_ARG;
import static org.graylog.plugins.pipelineprocessor.functions.FromInput.NAME_ARG;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;

public class PipelineAnalyzer {
    private final PipelineStreamConnectionsService connectionsService;
    private final InputService inputService;
    private final InputRegistry inputRegistry;
    private final PipelineMetricRegistry pipelineMetricRegistry;

    @Inject
    public PipelineAnalyzer(
            PipelineStreamConnectionsService connectionsService,
            InputService inputService,
            InputRegistry inputRegistry,
            MetricRegistry metricRegistry
    ) {
        this.connectionsService = connectionsService;
        this.inputService = inputService;
        this.inputRegistry = inputRegistry;
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
    }

    public Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> analyzePipelines(
            PipelineResolver resolver,
            List<PipelineRulesMetadataDao> ruleRecords) {
        final ImmutableMap<String, Pipeline> pipelines = resolver.resolvePipelines(pipelineMetricRegistry);
        final ImmutableMap<String, Pipeline> functions = resolver.resolveFunctions(pipelines.values(), pipelineMetricRegistry);
        return analyzePipelines(pipelines, functions, ruleRecords);
    }

    public Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> analyzePipelines(
            ImmutableMap<String, Pipeline> pipelines,
            ImmutableMap<String, Pipeline> functions,
            List<PipelineRulesMetadataDao> ruleRecords) {
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions = new HashMap<>();

        pipelines.values().forEach(pipeline -> {
            PipelineRulesMetadataDao.Builder rulesBuilder = PipelineRulesMetadataDao.builder().pipelineId(pipeline.id());
            Set<Stage> stages = functions.get(pipeline.id()).stages();

            Set<String> connectedStreams = connectionsService.loadByPipelineId(pipeline.id())
                    .stream()
                    .map(PipelineConnections::streamId)
                    .collect(Collectors.toSet());

            Set<String> ruleSet = new HashSet<>();
            Set<String> functionSet = new HashSet<>();
            Set<String> deprecatedFunctionSet = new HashSet<>();

            if (stages != null) {
                stages.forEach(stage -> {
                    final List<Rule> rules = stage.getRules();
                    if (rules != null) {
                        rules.forEach(rule -> {
                            if (rule != null) {
                                ruleSet.add(rule.id());
                                analyzeRule(pipeline, connectedStreams, stage, rule, functionSet, deprecatedFunctionSet, inputMentions);
                            }
                        });
                    }
                });
            }
            rulesBuilder
                    .rules(ruleSet)
                    .streams(connectedStreams)
                    .functions(functionSet)
                    .deprecatedFunctions(deprecatedFunctionSet);
            ruleRecords.add(rulesBuilder.build());
        });
        return inputMentions;
    }

    private void analyzeRule(Pipeline pipeline, Set<String> connectedStreams,
                             Stage stage, Rule rule,
                             Set<String> functions, Set<String> deprecatedFunctionList,
                             Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions) {
        MetaDataListener ruleListener = new MetaDataListener(pipeline, connectedStreams, stage, rule, inputMentions);
        new RuleAstWalker().walk(ruleListener, rule);
        functions.addAll(ruleListener.getFunctions());
        deprecatedFunctionList.addAll(ruleListener.getDeprecatedFunctions());
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
        private final Pipeline pipeline;
        private final Set<String> connectedStreams;
        private final Stage stage;
        private final Rule rule;
        private final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions;

        MetaDataListener(Pipeline pipeline, Set<String> connectedStreams, Stage stage, Rule rule,
                         Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions) {
            this.pipeline = pipeline;
            this.connectedStreams = connectedStreams;
            this.stage = stage;
            this.rule = rule;
            this.inputMentions = inputMentions;
        }

        @Override
        public void enterEquality(EqualityExpression expr) {
            Expression inputIdCandidate = null;
            if (expr.left().toString().contains(FIELD_GL2_SOURCE_INPUT)) {
                inputIdCandidate = expr.right();
            } else if (expr.right().toString().contains(FIELD_GL2_SOURCE_INPUT)) {
                inputIdCandidate = expr.left();
            }
            if (inputIdCandidate instanceof StringExpression stringExpression) {
                final String inputId = (String) stringExpression.evaluateUnsafe(null);
                if (isValidInputId(inputId)) {
                    createOrUpdateMention(inputId);
                }
            }
        }

        @Override
        public void enterFunctionCall(FunctionExpression expr) {
            final FunctionDescriptor<?> descriptor = expr.getFunction().descriptor();
            analyzeFunctions(descriptor);
            analyzeInputs(descriptor, expr.getArgs());
        }

        private void analyzeFunctions(FunctionDescriptor<?> descriptor) {
            functions.add(descriptor.name());
            if (Boolean.TRUE.equals(descriptor.deprecated())) {
                deprecatedFunctions.add(descriptor.name());
            }
        }

        private void analyzeInputs(FunctionDescriptor<?> descriptor, FunctionArgs args) {
            if (descriptor.name().equals("from_forwarder_input") || descriptor.name().equals("from_input")) {
                String inputId = null;
                if (args.getPreComputedValue(ID_ARG) != null) {
                    inputId = args.getPreComputedValue(ID_ARG).toString();
                } else if (args.getPreComputedValue(NAME_ARG) != null) {
                    inputId = inputRegistry.getIdByTitle(args.getPreComputedValue(NAME_ARG).toString());
                }
                if (inputId != null) {
                    createOrUpdateMention(inputId);
                }
            }
        }

        private void createOrUpdateMention(String inputId) {
            if (inputMentions.get(inputId) == null) {
                // first mention of this input
                final Set<PipelineInputsMetadataDao.MentionedInEntry> mentionedInEntries = Set.of(new PipelineInputsMetadataDao.MentionedInEntry(
                        pipeline.id(), rule.id(), Set.of(stage.stage()), connectedStreams));
                inputMentions.put(inputId, mentionedInEntries);
            } else {
                // update existing mention
                final Set<PipelineInputsMetadataDao.MentionedInEntry> mentionedInEntries = inputMentions.get(inputId);
                final Optional<PipelineInputsMetadataDao.MentionedInEntry> optMention = mentionedInEntries.stream()
                        .filter(e -> e.pipelineId().equals(pipeline.id()) && e.ruleId().equals(rule.id()))
                        .findFirst();
                if (optMention.isPresent()) {
                    optMention.get().connectedStreams().addAll(connectedStreams);
                    optMention.get().stages().add(stage.stage());
                } else {
                    mentionedInEntries.add(new PipelineInputsMetadataDao.MentionedInEntry(
                            pipeline.id(), rule.id(), Set.of((stage.stage())), connectedStreams));
                }
            }
        }

        public Set<String> getFunctions() {
            return functions;
        }

        public Set<String> getDeprecatedFunctions() {
            return deprecatedFunctions;
        }
    }

}
