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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.events.PipelinesChangedEvent;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

public class MongoDbPipelineService implements PipelineService {
    private static final RateLimitedLog log = getRateLimitedLog(MongoDbPipelineService.class);

    public static final String COLLECTION = "pipeline_processor_pipelines";

    private final MongoCollection<PipelineDao> collection;
    private final ClusterEventBus clusterBus;
    private final MongoUtils<PipelineDao> mongoUtils;

    @Inject
    public MongoDbPipelineService(MongoCollections mongoCollections,
                                  ClusterEventBus clusterBus) {
        this.collection = mongoCollections.collection(COLLECTION, PipelineDao.class);
        this.clusterBus = clusterBus;
        this.mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending("title"), new IndexOptions().unique(true));
    }

    @Override
    public PipelineDao save(PipelineDao pipeline) {
        final var pipelineId = pipeline.id();
        final PipelineDao savedPipeline;
        if (pipelineId != null) {
            collection.replaceOne(idEq(pipelineId), pipeline, new ReplaceOptions().upsert(true));
            savedPipeline = pipeline;
        } else {
            final var insertedId = insertedIdAsString(collection.insertOne(pipeline));
            savedPipeline = pipeline.toBuilder().id(insertedId).build();
        }

        clusterBus.post(PipelinesChangedEvent.updatedPipelineId(savedPipeline.id()));

        return savedPipeline;
    }

    @Override
    public PipelineDao load(String id) throws NotFoundException {
        return mongoUtils.getById(id).orElseThrow(() ->
                new NotFoundException("No pipeline with id " + id)
        );
    }

    @Override
    public PipelineDao loadByName(String name) throws NotFoundException {
        final PipelineDao pipeline = collection.find(eq("title", name)).first();
        if (pipeline == null) {
            throw new NotFoundException("No pipeline with name " + name);
        }
        return pipeline;
    }

    @Override
    public Collection<PipelineDao> loadAll() {
        try {
            return collection.find().into(new LinkedHashSet<>());
        } catch (MongoException e) {
            log.error("Unable to load pipelines", e);
            return Collections.emptySet();
        }
    }

    @Override
    public void delete(String id) {
        collection.deleteOne(idEq(id));
        clusterBus.post(PipelinesChangedEvent.deletedPipelineId(id));
    }

    public long count(Bson filter) {
        return collection.countDocuments(filter);
    }
}
