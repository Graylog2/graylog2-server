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
package org.graylog2.migrations;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableMap;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertOneModel;
import jakarta.annotation.Nonnull;
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
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.inputs.InputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.INPUTS_COLLECTION_NAME;
import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.RULES_COLLECTION_NAME;
import static org.graylog.plugins.pipelineprocessor.functions.FromInput.ID_ARG;
import static org.graylog.plugins.pipelineprocessor.functions.FromInput.NAME_ARG;
import static org.graylog2.plugin.Message.FIELD_GL2_SOURCE_INPUT;

/**
 * Migration to create the pipeline metadata collections, if any of them do not exist yet.
 */
public class V20251021083100_CreatePipelineMetadata extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251021083100_CreatePipelineMetadata.class);
    private final InputService inputService;
    private final PipelineStreamConnectionsService connectionsService;
    private final MongoDatabase db;
    private final PipelineResolver pipelineResolver;
    private final PipelineMetricRegistry pipelineMetricRegistry;
    private final MongoCollection<PipelineRulesMetadataDao> rulesCollection;
    private final MongoCollection<PipelineInputsMetadataDao> inputsCollection;

    @Inject
    public V20251021083100_CreatePipelineMetadata(MongoConnection mongoConnection,
                                                  MongoCollections mongoCollections,
                                                  InputService inputService,
                                                  PipelineService pipelineService,
                                                  RuleService ruleService,
                                                  PipelineStreamConnectionsService connectionsService,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  MetricRegistry metricRegistry,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.inputService = inputService;
        this.connectionsService = connectionsService;
        this.db = mongoConnection.getMongoDatabase();
        this.rulesCollection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        this.inputsCollection = mongoCollections.collection(INPUTS_COLLECTION_NAME, PipelineInputsMetadataDao.class);
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        this.pipelineResolver = pipelineResolverFactory.create(
                PipelineResolverConfig.of(
                        () -> ruleService.loadAll().stream(),
                        () -> pipelineService.loadAll().stream(),
                        () -> pipelineStreamConnectionsService.loadAll().stream()
                ),
                pipelineRuleParser
        );
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-10-21T08:31:00Z");
    }

    @Override
    public void upgrade() {
        if (collectionNotExists(RULES_COLLECTION_NAME) || collectionNotExists(INPUTS_COLLECTION_NAME)) {
            // rebuild all metadata collections from scratch
            db.getCollection(RULES_COLLECTION_NAME).drop();
            db.getCollection(INPUTS_COLLECTION_NAME).drop();

            createMetadata();
        }
    }

    private boolean collectionNotExists(String collectionName) {
        for (String name : db.listCollectionNames()) {
            if (name.equals(collectionName)) {
                return false;
            }
        }
        return true;
    }

    protected boolean isValidInputId(String inputId) {
        try {
            return (inputService.find(inputId) != null);
        } catch (NotFoundException e) {
            return false;
        }
    }

    protected String getInputId(String inputName) {
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

    private void createMetadata() {
        LOG.info("Creating pipeline metadata collection.");
        final List<InsertOneModel<PipelineRulesMetadataDao>> ruleRecords = new ArrayList<>();

        final ImmutableMap<String, Pipeline> pipelines = pipelineResolver.resolvePipelines(pipelineMetricRegistry);
        final ImmutableMap<String, Pipeline> functions = pipelineResolver.resolveFunctions(pipelines.values(), pipelineMetricRegistry);
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions = analyzePipelines(pipelines, functions, ruleRecords);

        final List<InsertOneModel<PipelineInputsMetadataDao>> inputRecords = new ArrayList<>();
        inputMentions.forEach((inputId, mentionedInEntries) -> {
            final PipelineInputsMetadataDao inputMetadata = PipelineInputsMetadataDao.builder()
                    .inputId(inputId)
                    .mentionedIn(new ArrayList<>(mentionedInEntries))
                    .build();
            inputRecords.add(new InsertOneModel<>(inputMetadata));
        });
        if (!inputRecords.isEmpty()) {
            LOG.info("Inserting {} pipeline inputs metadata records.", inputRecords.size());
            inputsCollection.bulkWrite(inputRecords);
        }

        if (!ruleRecords.isEmpty()) {
            LOG.info("Inserting {} pipeline rules metadata records.", ruleRecords.size());
            rulesCollection.bulkWrite(ruleRecords);
        }
    }

    @Nonnull
    private Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> analyzePipelines(
            ImmutableMap<String, Pipeline> pipelines,
            ImmutableMap<String, Pipeline> functions,
            List<InsertOneModel<PipelineRulesMetadataDao>> ruleRecords) {
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions = new HashMap<>();

        pipelines.values().forEach(pipeline -> {
            PipelineRulesMetadataDao.Builder rulesBuilder = PipelineRulesMetadataDao.builder().pipelineId(pipeline.id());
            Set<Stage> stages = functions.get(pipeline.id()).stages();

            Set<String> connectedStreams = connectionsService.loadByPipelineId(pipeline.id())
                    .stream()
                    .map(PipelineConnections::streamId)
                    .collect(Collectors.toSet());

            List<String> ruleList = new ArrayList<>();
            List<String> functionList = new ArrayList<>();
            List<String> deprecatedFunctionList = new ArrayList<>();

            if (stages != null) {
                stages.forEach(stage -> {
                    final List<Rule> rules = stage.getRules();
                    if (rules != null) {
                        rules.forEach(rule -> {
                            if (rule != null) {
                                ruleList.add(rule.id());
                                analyzeRule(pipeline, connectedStreams, stage, rule, functionList, deprecatedFunctionList, inputMentions);
                            }
                        });
                    }
                });
            }
            rulesBuilder.rules(ruleList).functions(functionList).deprecatedFunctions(deprecatedFunctionList);
            ruleRecords.add(new InsertOneModel<>(rulesBuilder.build()));
        });
        return inputMentions;
    }

    private void analyzeRule(Pipeline pipeline, Set<String> connectedStreams,
                             Stage stage, Rule rule,
                             List<String> functionList, List<String> deprecatedFunctionList,
                             Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions) {
        MetaDataListener ruleListener = new MetaDataListener(pipeline, connectedStreams, stage, rule, inputMentions);
        new RuleAstWalker().walk(ruleListener, rule);
        functionList.addAll(ruleListener.getFunctions());
        deprecatedFunctionList.addAll(ruleListener.getDeprecatedFunctions());
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
                    inputId = getInputId(args.getPreComputedValue(NAME_ARG).toString());
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
