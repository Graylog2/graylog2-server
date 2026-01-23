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
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbInputsMetadataService.INPUTS_COLLECTION_NAME;
import static org.graylog.plugins.pipelineprocessor.db.mongodb.MongoDbPipelineMetadataService.RULES_COLLECTION_NAME;

/**
 * Migration to create the pipeline metadata collections, if any of them do not exist yet.
 * Updated to include information routing rules and routed streams.
 */
public class V20251222123500_CreatePipelineMetadata extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251222123500_CreatePipelineMetadata.class);

    private final ClusterConfigService configService;
    private final MongoDatabase db;
    private final PipelineResolver pipelineResolver;
    private final PipelineAnalyzer pipelineAnalyzer;
    private final MongoDbPipelineMetadataService pipelineMetadataService;
    private final MongoDbInputsMetadataService inputsMetadataService;

    @Inject
    public V20251222123500_CreatePipelineMetadata(ClusterConfigService configService,
                                                  MongoConnection mongoConnection,
                                                  MongoDbRuleService ruleService,
                                                  MongoDbPipelineMetadataService pipelineMetadataService,
                                                  MongoDbInputsMetadataService inputsMetadataService,
                                                  PipelineService pipelineService,
                                                  PipelineAnalyzer pipelineAnalyzer,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.configService = configService;
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

    @Override
    public void upgrade() {
        if (migrationAlreadyApplied()) {
            return;
        }
        doUpgrade();
    }

    private void createMetadata() {
        LOG.info("Creating pipeline metadata collection.");
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

        markMigrationApplied();
    }

    private boolean migrationAlreadyApplied() {
        return Objects.nonNull(configService.get(V20251222123500_CreatePipelineMetadata.MigrationCompleted.class));
    }

    // The second migration marker indicates that schema has been upgraded to include routed_streams field
    private void markMigrationApplied() {
        configService.write(new V20251222123500_CreatePipelineMetadata.MigrationCompleted());
    }

    public record MigrationCompleted() {}
}
