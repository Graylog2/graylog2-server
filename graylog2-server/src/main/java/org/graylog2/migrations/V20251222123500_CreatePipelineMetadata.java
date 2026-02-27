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

import com.mongodb.client.MongoDatabase;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineInputsMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbRuleService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineAnalyzer;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService.INPUTS_COLLECTION_NAME;
import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.RULES_COLLECTION_NAME;

/**
 * Rebuilds the pipeline metadata collections on every startup. This ensures metadata stays in sync with the
 * primary pipeline, rule, and connection data even if incremental updates were missed or failed.
 */
public class V20251222123500_CreatePipelineMetadata extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251222123500_CreatePipelineMetadata.class);

    private final MongoDatabase db;
    private final PipelineResolver pipelineResolver;
    private final PipelineAnalyzer pipelineAnalyzer;
    private final MongoDbPipelineMetadataService pipelineMetadataService;
    private final MongoDbInputsMetadataService inputsMetadataService;

    @Inject
    public V20251222123500_CreatePipelineMetadata(MongoConnection mongoConnection,
                                                  MongoDbRuleService ruleService,
                                                  MongoDbPipelineMetadataService pipelineMetadataService,
                                                  MongoDbInputsMetadataService inputsMetadataService,
                                                  PipelineService pipelineService,
                                                  PipelineAnalyzer pipelineAnalyzer,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.db = mongoConnection.getMongoDatabase();
        this.pipelineMetadataService = pipelineMetadataService;
        this.inputsMetadataService = inputsMetadataService;
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
        return ZonedDateTime.parse("2025-12-22T12:35:00Z");
    }

    // This migration intentionally runs on every server restart (no MigrationCompleted guard).
    // Pipeline metadata is a derived cache built from pipelines, rules, and stream connections.
    // Incremental updates can silently fail or drift out of sync due to lost events, exceptions,
    // or partial writes. Rebuilding from scratch on startup guarantees consistency.
    @Override
    public void upgrade() {
        doUpgrade();
    }

    private void createMetadata() {
        LOG.info("Rebuilding pipeline metadata collections.");
        final List<PipelineRulesMetadataDao> ruleRecords = new ArrayList<>();
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions =
                pipelineAnalyzer.analyzePipelines(pipelineResolver, ruleRecords);

        pipelineMetadataService.save(ruleRecords, false);
        inputsMetadataService.save(inputMentions, false);
    }

    public void doUpgrade() {
        db.getCollection(RULES_COLLECTION_NAME).drop();
        db.getCollection(INPUTS_COLLECTION_NAME).drop();
        createMetadata();
    }
}
