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
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineAnalyzer;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.INPUTS_COLLECTION_NAME;
import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.RULES_COLLECTION_NAME;

/**
 * Migration to create the pipeline metadata collections, if any of them do not exist yet.
 */
public class V20251021083100_CreatePipelineMetadata extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251021083100_CreatePipelineMetadata.class);
    private final MongoDatabase db;
    private final PipelineResolver pipelineResolver;
    private final PipelineAnalyzer pipelineAnalyzer;
    private final PipelineMetricRegistry pipelineMetricRegistry;
    private final MongoCollection<PipelineRulesMetadataDao> rulesCollection;
    private final MongoCollection<PipelineInputsMetadataDao> inputsCollection;

    @Inject
    public V20251021083100_CreatePipelineMetadata(MongoConnection mongoConnection,
                                                  MongoCollections mongoCollections,
                                                  PipelineService pipelineService,
                                                  PipelineAnalyzer pipelineAnalyzer,
                                                  RuleService ruleService,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  MetricRegistry metricRegistry,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.db = mongoConnection.getMongoDatabase();
        this.rulesCollection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        this.inputsCollection = mongoCollections.collection(INPUTS_COLLECTION_NAME, PipelineInputsMetadataDao.class);
        this.pipelineMetricRegistry = PipelineMetricRegistry.create(metricRegistry, Pipeline.class.getName(), Rule.class.getName());
        this.pipelineAnalyzer = pipelineAnalyzer;
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

    private void createMetadata() {
        LOG.info("Creating pipeline metadata collection.");
        final List<InsertOneModel<PipelineRulesMetadataDao>> ruleRecords = new ArrayList<>();

        final ImmutableMap<String, Pipeline> pipelines = pipelineResolver.resolvePipelines(pipelineMetricRegistry);
        final ImmutableMap<String, Pipeline> functions = pipelineResolver.resolveFunctions(pipelines.values(), pipelineMetricRegistry);
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions =
                pipelineAnalyzer.analyzePipelines(pipelines, functions, ruleRecords);

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
}
