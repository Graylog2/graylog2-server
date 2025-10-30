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
package org.graylog.plugins.pipelineprocessor.db.mongodb;

import com.google.common.collect.ImmutableList;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.pipelineprocessor.db.PipelineRulesMetadataDao.FIELD_PIPELINE_ID;

/**
 * Persists information on pipeline and rules to avoid parsing these repeatedly
 */
public class MongoDbPipelineMetadataService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbPipelineMetadataService.class);
    public static final String RULES_COLLECTION_NAME = "pipeline_processor_rules_meta";
    public static final String INPUTS_COLLECTION_NAME = "pipeline_processor_inputs_meta";
    private final MongoCollection<PipelineRulesMetadataDao> collection;

    @Inject
    public MongoDbPipelineMetadataService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        collection.createIndex(Indexes.ascending(FIELD_PIPELINE_ID), new IndexOptions().unique(true));
    }

    public ImmutableList<PipelineRulesMetadataDao> getAll() {
        return ImmutableList.copyOf(collection.find());
    }

    public PipelineRulesMetadataDao getByPipelineId(final String pipelineId) throws NotFoundException {
        final PipelineRulesMetadataDao dao = collection.find(eq(FIELD_PIPELINE_ID, pipelineId)).first();
        if (dao == null) {
            throw new NotFoundException("No pipeline found with id: " + pipelineId);
        }
        return dao;
    }

    public Set<String> getPipelinesByRule(final String ruleId) {
        return collection.find(eq(PipelineRulesMetadataDao.FIELD_RULES, ruleId))
                .map(PipelineRulesMetadataDao::pipelineId)
                .into(new HashSet<>());
    }

}
