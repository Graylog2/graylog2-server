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
    private final MongoDbPipelineMetadataService metadataService;

    @Inject
    public V20251021083100_CreatePipelineMetadata(MongoConnection mongoConnection,
                                                  MongoDbRuleService ruleService,
                                                  MongoDbPipelineMetadataService metadataService,
                                                  PipelineService pipelineService,
                                                  PipelineAnalyzer pipelineAnalyzer,
                                                  PipelineStreamConnectionsService pipelineStreamConnectionsService,
                                                  PipelineRuleParser pipelineRuleParser,
                                                  PipelineResolver.Factory pipelineResolverFactory) {
        this.db = mongoConnection.getMongoDatabase();
        this.metadataService = metadataService;
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
        if (anyMissing(Set.of(RULES_COLLECTION_NAME, INPUTS_COLLECTION_NAME))) {
            // rebuild all metadata collections from scratch
            db.getCollection(RULES_COLLECTION_NAME).drop();
            db.getCollection(INPUTS_COLLECTION_NAME).drop();

            createMetadata();
        }
    }

    /**
     * Check if any of the specified collection candidates is missing.
     *
     * @param candidates check for existence of these collections
     * @return false, if all collections exist; true, if any collection is missing
     */
    private boolean anyMissing(Set<String> candidates) {
        final List<String> collections = db.listCollectionNames().into(new ArrayList<>());
        for (String name : candidates) {
            if (!collections.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private void createMetadata() {
        LOG.info("Creating pipeline metadata collection.");
        final List<PipelineRulesMetadataDao> ruleRecords = new ArrayList<>();
        final Map<String, Set<PipelineInputsMetadataDao.MentionedInEntry>> inputMentions =
                pipelineAnalyzer.analyzePipelines(pipelineResolver, ruleRecords);

        metadataService.saveRulesMetadata(ruleRecords, false);
        metadataService.saveInputsMetadata(inputMentions, false);
    }
}
