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

import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.swrve.ratelimitedlogger.RateLimitedLog;
import jakarta.inject.Inject;
import org.graylog.plugins.pipelineprocessor.db.PipelineStreamConnectionsService;
import org.graylog.plugins.pipelineprocessor.events.PipelineConnectionsChangedEvent;
import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter.getRateLimitedLog;

public class MongoDbPipelineStreamConnectionsService implements PipelineStreamConnectionsService {
    private static final RateLimitedLog log = getRateLimitedLog(MongoDbPipelineStreamConnectionsService.class);

    private static final String COLLECTION = "pipeline_processor_pipelines_streams";

    private final ClusterEventBus clusterBus;
    private final MongoCollection<PipelineConnections> collection;
    private final MongoUtils<PipelineConnections> mongoUtils;

    @Inject
    public MongoDbPipelineStreamConnectionsService(MongoCollections mongoCollections, ClusterEventBus clusterBus) {
        this.clusterBus = clusterBus;
        this.collection = mongoCollections.collection(COLLECTION, PipelineConnections.class);
        this.mongoUtils = mongoCollections.utils(collection);

        collection.createIndex(Indexes.ascending("stream_id"), new IndexOptions().unique(true));
    }

    @Override
    public PipelineConnections save(PipelineConnections connections) {
        PipelineConnections existingConnections = collection.find(eq("stream_id", connections.streamId()))
                .first();
        if (existingConnections == null) {
            existingConnections = PipelineConnections.create(null, connections.streamId(), Collections.emptySet());
        }

        final PipelineConnections toSave = existingConnections.toBuilder()
                .pipelineIds(connections.pipelineIds()).build();

        final PipelineConnections savedConnections = mongoUtils.save(toSave);
        clusterBus.post(PipelineConnectionsChangedEvent.create(savedConnections.streamId(), savedConnections.pipelineIds()));

        return savedConnections;
    }

    @Override
    public PipelineConnections load(String streamId) throws NotFoundException {
        final PipelineConnections oneById = collection.find(eq("stream_id", streamId)).first();
        if (oneById == null) {
            throw new NotFoundException("No pipeline connections for stream " + streamId);
        }
        return oneById;
    }

    @Override
    public Set<PipelineConnections> loadAll() {
        return ImmutableSet.copyOf(collection.find());
    }

    @Override
    public Set<PipelineConnections> loadByPipelineId(String pipelineId) {
        return ImmutableSet.copyOf(collection.find(in("pipeline_ids", pipelineId)));
    }

    @Override
    public void delete(String streamId) {
        try {
            final PipelineConnections connections = load(streamId);
            final Set<String> pipelineIds = connections.pipelineIds();

            mongoUtils.deleteById(connections.id());
            clusterBus.post(PipelineConnectionsChangedEvent.create(streamId, pipelineIds));
        } catch (NotFoundException e) {
            log.debug("No connections found for stream {}", streamId);
        }
    }

    @Override
    public Map<String, PipelineConnections> loadByStreamIds(Collection<String> streamIds) {
        try (final var stream = MongoUtils.stream(collection.find(in("stream_id", streamIds)))) {
            return stream.collect(Collectors.toMap(PipelineConnections::streamId, conn -> conn));
        }
    }
}
