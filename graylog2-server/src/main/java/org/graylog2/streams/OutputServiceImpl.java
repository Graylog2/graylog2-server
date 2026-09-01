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

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.outputs.MessageOutputFactory;
import org.graylog2.outputs.events.OutputChangedEvent;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.rest.models.streams.outputs.requests.CreateOutputRequest;
import org.graylog2.rest.resources.streams.outputs.AvailableOutputSummary;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.graylog2.database.utils.MongoUtils.idEq;

public class OutputServiceImpl implements OutputService {
    private static final Logger LOG = LoggerFactory.getLogger(OutputServiceImpl.class);

    private final StreamService streamService;
    private final ClusterEventBus clusterEventBus;
    private final MessageOutputFactory messageOutputFactory;
    private final ObjectMapper objectMapper;
    private final MongoCollection<OutputImpl> collection;
    private final MongoCollection<Document> rawCollection;

    @Inject
    public OutputServiceImpl(MongoCollections mongoCollections,
                             StreamService streamService,
                             ClusterEventBus clusterEventBus,
                             MessageOutputFactory messageOutputFactory,
                             ObjectMapper objectMapper) {
        this.streamService = streamService;
        this.messageOutputFactory = messageOutputFactory;
        this.objectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(this.objectMapper);
        final String collectionName = OutputImpl.class.getAnnotation(DbEntity.class).collection();
        this.collection = mongoCollections.nonEntityCollection(collectionName, OutputImpl.class);
        this.rawCollection = mongoCollections.nonEntityCollection(collectionName, Document.class);
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public Output load(String streamOutputId) throws NotFoundException {
        final OutputImpl output = collection.find(idEq(streamOutputId)).first();
        if (output == null) {
            throw new NotFoundException("Couldn't find output with id " + streamOutputId);
        }

        return withEncryptedFields(output);
    }

    @Override
    public Set<Output> loadAll() {
        final Map<String, AvailableOutputSummary> availableOutputs = messageOutputFactory.getAvailableOutputs();
        try (final var stream = MongoUtils.stream(collection.find())) {
            return stream.map(output -> withEncryptedFields(output, availableOutputs)).collect(ImmutableSet.toImmutableSet());
        }
    }

    @Override
    public Set<Output> loadByIds(Collection<String> ids) {
        final Map<String, AvailableOutputSummary> availableOutputs = messageOutputFactory.getAvailableOutputs();
        try (final var stream = MongoUtils.stream(collection.find(MongoUtils.stringIdsIn(ids)))) {
            return stream.map(output -> withEncryptedFields(output, availableOutputs)).collect(ImmutableSet.toImmutableSet());
        }
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

        return updatedOutput == null ? null : withEncryptedFields(updatedOutput);
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public Map<String, Long> countByType() {
        final Map<String, Long> outputsCountByType;
        try (final var stream = MongoUtils.stream(rawCollection.find()
                .projection(Projections.include(OutputImpl.FIELD_TYPE)))) {

            outputsCountByType = new HashMap<>(stream.map(doc -> doc.getString(OutputImpl.FIELD_TYPE))
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));
        }

        return outputsCountByType;
    }

    private OutputImpl implOrFail(Output output) {
        if (output instanceof OutputImpl outputImpl) {
            return outputImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type OutputImpl, not " + output.getClass());
        }
    }

    private Set<String> getEncryptedFields(String type, Map<String, AvailableOutputSummary> availableOutputs) {
        final AvailableOutputSummary summary = availableOutputs.get(type);
        if (summary == null) {
            return Set.of();
        }
        return EncryptedInputConfigs.getEncryptedFields(summary.requestedConfiguration());
    }

    private Output withEncryptedFields(OutputImpl output) {
        return withEncryptedFields(output, messageOutputFactory.getAvailableOutputs());
    }

    /**
     * Converts the raw {@code {encrypted_value, salt}} sub-documents stored in MongoDB back into {@link EncryptedValue}
     * objects for those configuration fields declared as encrypted. This ensures secrets are masked in API responses
     * and exposed as {@link EncryptedValue} to running outputs.
     */
    private Output withEncryptedFields(OutputImpl output, Map<String, AvailableOutputSummary> availableOutputs) {
        final Set<String> encryptedFields = getEncryptedFields(output.getType(), availableOutputs);
        if (encryptedFields.isEmpty()) {
            return output;
        }
        final Map<String, Object> originalConfig = output.getConfiguration();
        if (originalConfig == null || originalConfig.isEmpty()) {
            return output;
        }

        boolean modified = false;
        final Map<String, Object> newConfig = new HashMap<>(originalConfig);
        for (String field : encryptedFields) {
            final Object raw = newConfig.get(field);
            if (raw != null && !(raw instanceof EncryptedValue)) {
                try {
                    newConfig.put(field, objectMapper.convertValue(raw, EncryptedValue.class));
                    modified = true;
                } catch (IllegalArgumentException e) {
                    // Values written before encryption support was added are left untouched. Logged at debug because
                    // this repeats on every load and the operator can only fix it by re-entering the secret.
                    LOG.debug("Failed to convert field '{}' to EncryptedValue for output '{}': {}", field, output.getId(), e.getMessage());
                }
            }
        }
        return modified
                ? OutputImpl.create(output.getId(), output.getTitle(), output.getType(), output.getCreatorUserId(),
                newConfig, output.getCreatedAt(), output.getContentPack())
                : output;
    }
}
