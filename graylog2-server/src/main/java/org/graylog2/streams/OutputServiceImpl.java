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
package org.graylog2.streams;

import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.outputs.events.OutputChangedEvent;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static org.graylog2.database.utils.MongoUtils.insertedIdAsString;

public class OutputServiceImpl implements OutputService {

    private final MongoCollection<OutputImpl> collection;
    private final MongoUtils<OutputImpl> mongoUtils;
    private final StreamService streamService;
    private final ClusterEventBus clusterEventBus;


    @Inject
    public OutputServiceImpl(MongoCollections mongoCollections,
                             StreamService streamService,
                             ClusterEventBus clusterEventBus) {
        this.streamService = streamService;
        final String collectionName = OutputImpl.class.getAnnotation(DbEntity.class).collection();
        this.collection = mongoCollections.collection(collectionName, OutputImpl.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public Output load(String streamOutputId) throws NotFoundException {
        final Optional<OutputImpl> output = mongoUtils.getById(streamOutputId);
        if (output.isEmpty()) {
            throw new NotFoundException("Couldn't find output with id " + streamOutputId);
        }

        return output.get();
    }

    @Override
    public Set<Output> loadAll() {
        return ImmutableSet.copyOf(collection.find());
    }

    @Override
    public Set<Output> loadByIds(Collection<String> ids) {
        List<ObjectId> objIds = ids.stream()
                .map(ObjectId::new)
                .toList();
        final Bson query = in(OutputImpl.FIELD_ID, objIds);
        return ImmutableSet.copyOf(collection.find(query));
    }

    @Override
    public Output create(Output request) throws ValidationException {
        final OutputImpl outputImpl = implOrFail(request);
        final String id = insertedIdAsString(collection.insertOne(outputImpl));

        return OutputImpl.create(id,
                outputImpl.getTitle(),
                outputImpl.getType(),
                outputImpl.getCreatorUserId(),
                outputImpl.getConfiguration(),
                outputImpl.getCreatedAt(),
                outputImpl.getContentPack());
    }

    @Override
    public Output create(CreateOutputRequest request, String userId) throws ValidationException {
        return create(OutputImpl.create(new ObjectId().toHexString(), request.title(), request.type(), userId, request.configuration(),
                Tools.nowUTC().toDate(), request.contentPack()));
    }

    @Override
    public void destroy(Output model) throws NotFoundException {
        mongoUtils.deleteById(model.getId());

        // Removing the output from all streams will emit a StreamsChangedEvent for affected streams.
        // The OutputRegistry will handle this event and stop the output.
        streamService.removeOutputFromAllStreams(model);
    }

    @Override
    public Output update(String id, Map<String, Object> deltas) {
        List<Bson> updates = new ArrayList<>();
        for (Map.Entry<String, Object> field : deltas.entrySet()) {
            updates.add(Updates.set(field.getKey(), field.getValue()));
        }

        UpdateResult result = collection.updateOne(eq(OutputImpl.FIELD_ID, new ObjectId(id)), Updates.combine(updates));
        if (result.getUpsertedId() != null) {
            final Optional<OutputImpl> updatedOutput = mongoUtils.getById(id);
            if (updatedOutput.isEmpty()) {
                return null;
            }
            this.clusterEventBus.post(OutputChangedEvent.create(updatedOutput.get().getId()));

            return updatedOutput.get();
        }
        return null;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public Map<String, Long> countByType() {
        final Map<String, Long> outputsCountByType = new HashMap<>();
        final Iterable<OutputImpl> outputs = collection.find();

        for (OutputImpl output : outputs) {
            final String type = output.getType();
            if (type != null) {
                final Long oldValue = outputsCountByType.getOrDefault(type, 0L);
                outputsCountByType.put(type, oldValue + 1);
            }
        }

        return outputsCountByType;
    }

    private OutputImpl implOrFail(Output output) {
        final OutputImpl outputImpl;
        if (output instanceof OutputImpl) {
            outputImpl = (OutputImpl) output;
            return outputImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type OutputImpl, not " + output.getClass());
        }
    }
}
