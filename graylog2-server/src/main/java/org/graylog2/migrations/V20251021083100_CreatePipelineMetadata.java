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
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineMetricRegistry;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolver;
import org.graylog.plugins.pipelineprocessor.processors.PipelineResolverConfig;
import org.graylog2.database.MongoConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

/**
 * Migration creating the pipeline metadata collection if it doesn't exist.
 */
public class V20251021083100_CreatePipelineMetadata extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251021083100_CreatePipelineMetadata.class);
    private final PipelineService pipelineService;
    private final MongoDatabase db;
    private final PipelineResolver pipelineResolver;
    private final PipelineMetricRegistry pipelineMetricRegistry;

    @Inject
    public V20251021083100_CreatePipelineMetadata(MongoConnection mongoConnection,
                                                  RuleService ruleService,
                                                  PipelineService pipelineService,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  MetricRegistry metricRegistry,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.pipelineService = pipelineService;
        this.db = mongoConnection.getMongoDatabase();
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
//       if (!collectionExists()) {
        createMetadata();
//        }
    }

    private boolean collectionExists() {
        for (String name : db.listCollectionNames()) {
            if (name.equals(MongoDbPipelineMetadataService.COLLECTION_NAME)) {
                return true;
            }
        }
        return false;
    }

    private void createMetadata() {
        LOG.info("Creating pipeline metadata collection.");
        pipelineService.loadAll().forEach(this::analyzePipeline);
    }

    private void analyzePipeline(PipelineDao pipeline) {
        final ImmutableMap<String, Pipeline> pipelines = pipelineResolver.resolvePipelines(pipelineMetricRegistry);
        final ImmutableMap<String, Pipeline> functions = pipelineResolver.resolveFunctions(pipelines.values(), pipelineMetricRegistry);
    }
}
