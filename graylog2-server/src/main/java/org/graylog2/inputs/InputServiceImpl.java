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
package org.graylog2.inputs;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.inputs.extractors.events.ExtractorCreated;
import org.graylog2.inputs.extractors.events.ExtractorDeleted;
import org.graylog2.inputs.extractors.events.ExtractorUpdated;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InputServiceImpl extends PersistedServiceImpl implements InputService {
    private static final Logger LOG = LoggerFactory.getLogger(InputServiceImpl.class);

    private final ExtractorFactory extractorFactory;
    private final ConverterFactory converterFactory;
    private final MessageInputFactory messageInputFactory;
    private final EventBus clusterEventBus;
    private final DBCollection dbCollection;

    @Inject
    public InputServiceImpl(MongoConnection mongoConnection,
                            ExtractorFactory extractorFactory,
                            ConverterFactory converterFactory,
                            MessageInputFactory messageInputFactory,
                            ClusterEventBus clusterEventBus) {
        super(mongoConnection);
        this.extractorFactory = extractorFactory;
        this.converterFactory = converterFactory;
        this.messageInputFactory = messageInputFactory;
        this.clusterEventBus = clusterEventBus;
        this.dbCollection = collection(InputImpl.class);
    }

    @Override
    public List<Input> all() {
        final List<DBObject> ownInputs = query(InputImpl.class, new BasicDBObject());

        final ImmutableList.Builder<Input> inputs = ImmutableList.builder();
        for (final DBObject o : ownInputs) {
            inputs.add(new InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap()));
        }

        return inputs.build();
    }

    @Override
    public List<Input> allOfThisNode(final String nodeId) {
        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));
        final List<DBObject> ownInputs = query(InputImpl.class, new BasicDBObject("$or", query));

        final ImmutableList.Builder<Input> inputs = ImmutableList.builder();
        for (final DBObject o : ownInputs) {
            inputs.add(new InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap()));
        }

        return inputs.build();
    }

    @Override
    public Set<Input> findByIds(Collection<String> ids) {
        final Set<ObjectId> objectIds = ids.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());

        final DBObject query = BasicDBObjectBuilder.start()
                .push(InputImpl.FIELD_ID)
                .append("$in", objectIds)
                .get();
        final Stream<InputImpl> inputStream = query(InputImpl.class, query).stream()
                .map(o -> new InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap()));
        return inputStream
                .collect(Collectors.toSet());
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        final String resultId = super.save(model);
        if (resultId != null && !resultId.isEmpty()) {
            publishChange(InputCreated.create(resultId));
        }
        return resultId;
    }

    @Override
    public String update(Input model) throws ValidationException {
        final String resultId = super.save(model);
        if (resultId != null && !resultId.isEmpty()) {
            publishChange(InputUpdated.create(resultId));
        }
        return resultId;
    }

    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        final String resultId = super.saveWithoutValidation(model);
        if (resultId != null && !resultId.isEmpty()) {
            publishChange(InputCreated.create(resultId));
        }
        return resultId;
    }

    @Override
    public <T extends Persisted> int destroy(T model) {
        final int result = super.destroy(model);
        if (result > 0) {
            publishChange(InputDeleted.create(model.getId()));
        }
        return result;
    }

    @Override
    public Input create(String id, Map<String, Object> fields) {
        return new InputImpl(new ObjectId(id), fields);
    }

    @Override
    public Input create(Map<String, Object> fields) {
        return new InputImpl(fields);
    }

    @Override
    public Input find(String id) throws NotFoundException {
        if (!ObjectId.isValid(id)) {
            throw new NotFoundException("Input id <" + id + "> is invalid!");
        }
        final DBObject o = get(org.graylog2.inputs.InputImpl.class, id);
        if (o == null) {
            throw new NotFoundException("Input <" + id + "> not found!");
        }
        return new org.graylog2.inputs.InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap());
    }

    @Override
    public Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException {
        final List<BasicDBObject> forThisNodeOrGlobal = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));

        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(InputImpl.FIELD_ID, new ObjectId(id)),
                new BasicDBObject("$or", forThisNodeOrGlobal));

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        return new InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap());
    }

    @Override
    public Input findForThisNode(String nodeId, String id) throws NotFoundException, IllegalArgumentException {
        final List<BasicDBObject> forThisNode = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, false));

        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(InputImpl.FIELD_ID, new ObjectId(id)),
                new BasicDBObject("$and", forThisNode));

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        if (o == null) {
            throw new NotFoundException("Couldn't find input " + id + " on Graylog node " + nodeId);
        } else {
            return new InputImpl((ObjectId) o.get(InputImpl.FIELD_ID), o.toMap());
        }
    }

    @Override
    public void addExtractor(Input input, Extractor extractor) throws ValidationException {
        embed(input, InputImpl.EMBEDDED_EXTRACTORS, extractor);
        publishChange(ExtractorCreated.create(input.getId(), extractor.getId()));
    }

    @Override
    public void updateExtractor(Input input, Extractor extractor) throws ValidationException {
        removeEmbedded(input, InputImpl.EMBEDDED_EXTRACTORS, extractor.getId());
        embed(input, InputImpl.EMBEDDED_EXTRACTORS, extractor);
        publishChange(ExtractorUpdated.create(input.getId(), extractor.getId()));
    }

    @Override
    public void addStaticField(Input input, final String key, final String value) throws ValidationException {
        final EmbeddedPersistable obj = () -> ImmutableMap.of(
                InputImpl.FIELD_STATIC_FIELD_KEY, key,
                InputImpl.FIELD_STATIC_FIELD_VALUE, value);

        embed(input, InputImpl.EMBEDDED_STATIC_FIELDS, obj);
        publishChange(InputUpdated.create(input.getId()));
    }

    @Override
    public List<Map.Entry<String, String>> getStaticFields(Input input) {
        if (input.getFields().get(InputImpl.EMBEDDED_STATIC_FIELDS) == null) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<Map.Entry<String, String>> listBuilder = ImmutableList.builder();
        final BasicDBList mSF = (BasicDBList) input.getFields().get(InputImpl.EMBEDDED_STATIC_FIELDS);
        for (final Object element : mSF) {
            final DBObject ex = (BasicDBObject) element;
            try {
                final Map.Entry<String, String> staticField =
                        Maps.immutableEntry((String) ex.get(InputImpl.FIELD_STATIC_FIELD_KEY),
                                (String) ex.get(InputImpl.FIELD_STATIC_FIELD_VALUE));
                listBuilder.add(staticField);
            } catch (Exception e) {
                LOG.error("Cannot build static field from persisted data. Skipping.", e);
            }
        }

        return listBuilder.build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Extractor> getExtractors(Input input) {
        if (input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS) == null) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<Extractor> listBuilder = ImmutableList.builder();
        final BasicDBList mEx = (BasicDBList) input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS);
        for (final Object element : mEx) {
            final DBObject ex = (BasicDBObject) element;

            // SOFT MIGRATION: does this extractor have an order set? Implemented for issue: #726
            Long order = 0L;
            if (ex.containsField(Extractor.FIELD_ORDER)) {
                /* We use json format to describe our test fixtures
                   This format will only return Integer on this place,
                   which can't be converted to long. So I first cast
                   it to Number and eventually to long */
                Number num = (Number) ex.get(Extractor.FIELD_ORDER);
                order = num.longValue(); // mongodb driver gives us a java.lang.Long
            }

            try {
                final Extractor extractor = extractorFactory.factory(
                        (String) ex.get(Extractor.FIELD_ID),
                        (String) ex.get(Extractor.FIELD_TITLE),
                        order.intValue(),
                        Extractor.CursorStrategy.valueOf(((String) ex.get(Extractor.FIELD_CURSOR_STRATEGY)).toUpperCase(Locale.ENGLISH)),
                        Extractor.Type.valueOf(((String) ex.get(Extractor.FIELD_TYPE)).toUpperCase(Locale.ENGLISH)),
                        (String) ex.get(Extractor.FIELD_SOURCE_FIELD),
                        (String) ex.get(Extractor.FIELD_TARGET_FIELD),
                        (Map<String, Object>) ex.get(Extractor.FIELD_EXTRACTOR_CONFIG),
                        (String) ex.get(Extractor.FIELD_CREATOR_USER_ID),
                        getConvertersOfExtractor(ex),
                        Extractor.ConditionType.valueOf(((String) ex.get(Extractor.FIELD_CONDITION_TYPE)).toUpperCase(Locale.ENGLISH)),
                        (String) ex.get(Extractor.FIELD_CONDITION_VALUE)
                );

                listBuilder.add(extractor);
            } catch (Exception e) {
                LOG.error("Cannot build extractor from persisted data. Skipping.", e);
            }
        }

        return listBuilder.build();
    }

    @Override
    public Extractor getExtractor(final Input input, final String extractorId) throws NotFoundException {
        final Optional<Extractor> extractor = Iterables.tryFind(this.getExtractors(input), new Predicate<Extractor>() {
            @Override
            public boolean apply(Extractor extractor) {
                return extractor.getId().equals(extractorId);
            }
        });

        if (!extractor.isPresent()) {
            LOG.error("Extractor <{}> not found.", extractorId);
            throw new NotFoundException("Couldn't find extractor " + extractorId);
        }

        return extractor.get();
    }

    @SuppressWarnings("unchecked")
    private List<Converter> getConvertersOfExtractor(DBObject extractor) {
        final ImmutableList.Builder<Converter> listBuilder = ImmutableList.builder();

        final BasicDBList converters = (BasicDBList) extractor.get(Extractor.FIELD_CONVERTERS);
        for (final Object element : converters) {
            final DBObject c = (BasicDBObject) element;

            try {
                listBuilder.add(converterFactory.create(
                        Converter.Type.valueOf(((String) c.get(Extractor.FIELD_CONVERTER_TYPE)).toUpperCase(Locale.ENGLISH)),
                        (Map<String, Object>) c.get(Extractor.FIELD_CONVERTER_CONFIG)
                ));
            } catch (ConverterFactory.NoSuchConverterException e1) {
                LOG.error("Cannot build converter from persisted data. No such converter.", e1);
            } catch (Exception e) {
                LOG.error("Cannot build converter from persisted data.", e);
            }
        }

        return listBuilder.build();
    }

    @Override
    public void removeExtractor(Input input, String extractorId) {
        removeEmbedded(input, InputImpl.EMBEDDED_EXTRACTORS, extractorId);
        publishChange(ExtractorDeleted.create(input.getId(), extractorId));
    }

    @Override
    public void removeStaticField(Input input, String key) {
        removeEmbedded(input, InputImpl.FIELD_STATIC_FIELD_KEY, InputImpl.EMBEDDED_STATIC_FIELDS, key);
        publishChange(InputUpdated.create(input.getId()));
    }

    @Override
    public MessageInput getMessageInput(Input io) throws NoSuchInputTypeException {
        final Configuration configuration = new Configuration(io.getConfiguration());
        final MessageInput input = messageInputFactory.create(io.getType(), configuration);

        // Add all standard fields.
        input.setTitle(io.getTitle());
        input.setNodeId(io.getNodeId());
        input.setCreatorUserId(io.getCreatorUserId());
        input.setPersistId(io.getId());
        input.setCreatedAt(io.getCreatedAt());
        input.setContentPack(io.getContentPack());

        if (io.isGlobal()) {
            input.setGlobal(true);
        }

        // Add static fields.
        input.addStaticFields(io.getStaticFields());

        return input;
    }

    @Override
    public long totalCount() {
        return totalCount(InputImpl.class);
    }

    @Override
    public long globalCount() {
        return count(InputImpl.class, new BasicDBObject(MessageInput.FIELD_GLOBAL, true));
    }

    @Override
    public long localCount() {
        return count(InputImpl.class, new BasicDBObject(MessageInput.FIELD_GLOBAL, false));
    }

    @Override
    public Map<String, Long> totalCountByType() {
        final Map<String, Long> inputCountByType = new HashMap<>();
        try (DBCursor inputTypes = dbCollection.find(null, new BasicDBObject(MessageInput.FIELD_TYPE, 1))) {
            for (DBObject inputType : inputTypes) {
                final String type = (String) inputType.get(MessageInput.FIELD_TYPE);
                if (type != null) {
                    final Long oldValue = inputCountByType.get(type);
                    final Long newValue = (oldValue == null) ? 1 : oldValue + 1;
                    inputCountByType.put(type, newValue);
                }
            }
        }

        return inputCountByType;
    }

    @Override
    public long localCountForNode(String nodeId) {
        final List<BasicDBObject> forThisNode = ImmutableList.of(new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId));

        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_GLOBAL, false),
                new BasicDBObject("$or", forThisNode));

        return count(InputImpl.class, new BasicDBObject("$and", query));
    }

    @Override
    public long totalCountForNode(String nodeId) {
        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true),
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId));

        return count(InputImpl.class, new BasicDBObject("$or", query));
    }

    @Override
    public long totalExtractorCount() {
        final DBObject query = new BasicDBObject(InputImpl.EMBEDDED_EXTRACTORS, new BasicDBObject("$exists", true));
        long extractorsCount = 0;
        try (DBCursor inputs = dbCollection.find(query, new BasicDBObject(InputImpl.EMBEDDED_EXTRACTORS, 1))) {
            for (DBObject input : inputs) {
                final BasicDBList extractors = (BasicDBList) input.get(InputImpl.EMBEDDED_EXTRACTORS);
                extractorsCount += extractors.size();
            }
        }

        return extractorsCount;
    }

    @Override
    public Map<Extractor.Type, Long> totalExtractorCountByType() {
        final DBObject query = new BasicDBObject(InputImpl.EMBEDDED_EXTRACTORS, new BasicDBObject("$exists", true));
        try (DBCursor inputs = dbCollection.find(query, new BasicDBObject(InputImpl.EMBEDDED_EXTRACTORS, 1))) {
            final Map<Extractor.Type, Long> extractorsCountByType = new HashMap<>();
            for (DBObject input : inputs) {
                final BasicDBList extractors = (BasicDBList) input.get(InputImpl.EMBEDDED_EXTRACTORS);
                for (Object dbObject : extractors) {
                    final DBObject extractor = (DBObject) dbObject;
                    final Extractor.Type type = Extractor.Type.fuzzyValueOf(((String) extractor.get(Extractor.FIELD_TYPE)));
                    if (type != null) {
                        final Long oldValue = extractorsCountByType.get(type);
                        final Long newValue = (oldValue == null) ? 1 : oldValue + 1;
                        extractorsCountByType.put(type, newValue);
                    }
                }
            }
            return extractorsCountByType;
        }
    }

    private void publishChange(Object event) {
        this.clusterEventBus.post(event);
    }
}
