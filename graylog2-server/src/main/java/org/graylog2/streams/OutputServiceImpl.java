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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.bson.Document;
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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.graylog2.database.utils.MongoUtils.idEq;

public class OutputServiceImpl implements OutputService {
    private final StreamService streamService;
    private final ClusterEventBus clusterEventBus;
    private final MongoCollection<OutputImpl> collection;
    private final MongoCollection<Document> rawCollection;

    @Inject
    public OutputServiceImpl(MongoCollections mongoCollections,
                             StreamService streamService,
                             ClusterEventBus clusterEventBus) {
        this.streamService = streamService;
        final String collectionName = OutputImpl.class.getAnnotation(DbEntity.class).collection();
        this.collection = mongoCollections.nonEntityCollection(collectionName, OutputImpl.class);
        this.rawCollection = collection.withDocumentClass(Document.class);
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public Output load(String streamOutputId) throws NotFoundException {
        final Output output = collection.find(idEq(streamOutputId)).first();
        if (output == null) {
            throw new NotFoundException("Couldn't find output with id " + streamOutputId);
        }

        return output;
    }

    @Override
    public Set<Output> loadAll() {
        return ImmutableSet.copyOf(collection.find());
    }

    @Override
    public Set<Output> loadByIds(Collection<String> ids) {
        return ImmutableSet.copyOf(collection.find(MongoUtils.stringIdsIn(ids)));
    }

    @Override
    public Output create(Output output) throws ValidationException {
        final OutputImpl outputImpl = implOrFail(output);
        if (output.getId() == null) {
            final var insertedId = MongoUtils.insertedIdAsString(collection.insertOne(outputImpl));
            return OutputImpl.create(insertedId, outputImpl.getTitle(), outputImpl.getType(),
                    outputImpl.getCreatorUserId(), outputImpl.getConfiguration(), outputImpl.getCreatedAt(),
                    outputImpl.getContentPack());
        }
        collection.replaceOne(idEq(outputImpl.getId()), outputImpl, new ReplaceOptions().upsert(true));
        return outputImpl;
    }

    @Override
    public Output create(CreateOutputRequest request, String userId) throws ValidationException {
        return create(OutputImpl.create(new ObjectId().toHexString(), request.title(), request.type(), userId, request.configuration(),
                Tools.nowUTC().toDate(), request.contentPack()));
    }

    @Override
    public void destroy(Output model) throws NotFoundException {
        collection.deleteOne(idEq(model.getId()));

        // Removing the output from all streams will emit a StreamsChangedEvent for affected streams.
        // The OutputRegistry will handle this event and stop the output.
        streamService.removeOutputFromAllStreams(model);
    }

    @Override
    public Output update(String id, Map<String, Object> deltas) {
        final List<Bson> updates = deltas.entrySet().stream()
                .map(field -> Updates.set(field.getKey(), field.getValue()))
                .toList();

        final OutputImpl updatedOutput = collection.findOneAndUpdate(idEq(id), Updates.combine(updates),
                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));

        if (updatedOutput != null) {
            this.clusterEventBus.post(OutputChangedEvent.create(updatedOutput.getId()));
        }

        return updatedOutput;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public Map<String, Long> countByType() {
        final Map<String, Long> outputsCountByType = new HashMap<>();
        try (final var stream = MongoUtils.stream(rawCollection.find()
                .projection(Projections.include(OutputImpl.FIELD_TYPE)))) {

            stream.forEach(outputType -> {
                final String type = (String) outputType.get(OutputImpl.FIELD_TYPE);
                if (type != null) {
                    final Long oldValue = outputsCountByType.get(type);
                    final Long newValue = (oldValue == null) ? 1 : oldValue + 1;
                    outputsCountByType.put(type, newValue);
                }
            });
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
