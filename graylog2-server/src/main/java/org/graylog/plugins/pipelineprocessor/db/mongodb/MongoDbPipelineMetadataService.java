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

import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

/**
 * Persists information on pipeline and rules to avoid parsing these repeatedly
 */
public class MongoDbPipelineMetadataService {
    public static final String RULES_COLLECTION_NAME = "pipeline_processor_rules_meta";
    public static final String INPUTS_COLLECTION_NAME = "pipeline_processor_inputs_meta";
    private final MongoCollection<PipelineRulesMetadataDao> collection;

    @Inject
    public MongoDbPipelineMetadataService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(RULES_COLLECTION_NAME, PipelineRulesMetadataDao.class);
        collection.createIndex(Indexes.ascending(PipelineRulesMetadataDao.FIELD_PIPELINE_ID), new IndexOptions().unique(true));
    }

    public ImmutableList<PipelineRulesMetadataDao> getAll() {
        return ImmutableList.copyOf(collection.find());
    }

    public Optional<PipelineRulesMetadataDao> getByPipelineId(final String pipelineId) {
        return Optional.ofNullable(collection.find(eq(PipelineRulesMetadataDao.FIELD_PIPELINE_ID, pipelineId)).first());
    }

}
