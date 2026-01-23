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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.encryption.EncryptedInputConfigs;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.inputs.extractors.events.ExtractorCreated;
import org.graylog2.inputs.extractors.events.ExtractorDeleted;
import org.graylog2.inputs.extractors.events.ExtractorUpdated;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.rest.models.system.inputs.responses.InputDeleted;
import org.graylog2.rest.models.system.inputs.responses.InputUpdated;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Filters.regex;
import static org.graylog2.inputs.InputImpl.FIELD_CREATED_AT;

public class InputServiceImpl implements InputService {
    private static final Logger LOG = LoggerFactory.getLogger(InputServiceImpl.class);
    public static final String COLLECTION_NAME = "inputs";

    private final ExtractorFactory extractorFactory;
    private final ConverterFactory converterFactory;
    private final MessageInputFactory messageInputFactory;
    private final EventBus clusterEventBus;
    private final MongoCollection<InputImpl> collection;
    private final com.mongodb.client.MongoCollection<Document> documentCollection;
    private final MongoUtils<InputImpl> mongoUtils;
    private final MongoPaginationHelper<InputImpl> paginationHelper;
    private final ObjectMapper objectMapper;

    @Inject
    public InputServiceImpl(MongoCollections mongoCollections,
                            ExtractorFactory extractorFactory,
                            ConverterFactory converterFactory,
                            MessageInputFactory messageInputFactory,
                            ClusterEventBus clusterEventBus,
                            ObjectMapper objectMapper) {
        this.extractorFactory = extractorFactory;
        this.converterFactory = converterFactory;
        this.messageInputFactory = messageInputFactory;
        this.clusterEventBus = clusterEventBus;
        this.collection = mongoCollections.collection(COLLECTION_NAME, InputImpl.class);
        this.documentCollection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.objectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(this.objectMapper);
    }

    @Override
    public List<Input> all() {
        final ImmutableList.Builder<Input> result = ImmutableList.builder();
        collection.find().forEach(result::add);

        return result.build();
    }

    @Override
    public PaginatedList<Input> paginated(Bson searchQuery,
                                          Predicate<InputImpl> filter,
                                          SortOrder order,
                                          String sortField,
                                          int page,
                                          int perPage) {
        final PaginatedList<InputImpl> pagedListResponse = paginationHelper.perPage(perPage)
                .sort(order.toBsonSort(sortField))
                .filter(searchQuery)
                .page(page, filter);
        final List<Input> inputs = new ArrayList<>(pagedListResponse.stream().map(this::withEncryptedFields).toList());
        return new PaginatedList<>(inputs, pagedListResponse.pagination().total(), pagedListResponse.pagination().page(), pagedListResponse.pagination().perPage());
    }

    @Override
    public List<Input> allOfThisNode(final String nodeId) {
        final ImmutableList.Builder<Input> result = ImmutableList.builder();
        collection.find(or(
                eq(MessageInput.FIELD_NODE_ID, nodeId),
                eq(MessageInput.FIELD_GLOBAL, true)
        )).forEach(e -> result.add(withEncryptedFields(e)));

        return result.build();
    }

    @Override
    public List<Input> allByType(final String type) {
        final ImmutableList.Builder<Input> result = ImmutableList.builder();
        collection.find(eq(MessageInput.FIELD_TYPE, type)).forEach(e -> result.add(withEncryptedFields(e)));

        return result.build();
    }

    /**
     * Finds input IDs by title using a case-insensitive regex search. This is designed to mimic
     * InputRegistry::findByTitle behavior.
     * Regex takes advantage of MongoDB indexes, making this method efficient for large datasets.
     */
    @Override
    public List<String> findIdsByTitle(String title) {
        final ImmutableList.Builder<String> result = ImmutableList.builder();
        collection.find(regex(InputImpl.FIELD_TITLE, title, "i")).forEach(input -> result.add(input.getId()));
        return result.build();
    }

    @Override
    public Set<Input> findByIds(Collection<String> ids) {
        final Set<Input> result = new HashSet<>();
        mongoUtils.getByIds(ids).forEach(e -> result.add(withEncryptedFields(e)));

        return result;
    }

    public String save(Input model) throws ValidationException {
        return save(model, true);
    }

    @Override
    public String saveWithoutEvents(Input model) throws ValidationException {
        return save(model, false);
    }

    private InputImpl toInputImpl(Input input) {
        if (input instanceof InputImpl inputImpl) {
            return inputImpl;
        }
        throw new IllegalArgumentException("Expected InputImpl, got " + input.getClass().getName());
    }

    private String save(Input model, boolean fireEvents) throws ValidationException {
        validateStaticFields(model);
        final InputImpl input = toInputImpl(model);
        String inputId = input.getId();
        boolean isNew = (inputId == null || inputId.isBlank());

        if (isNew) {
            inputId = MongoUtils.insertedIdAsString(collection.insertOne(input));
        } else {
            collection.replaceOne(MongoUtils.idEq(inputId), input);
        }

        if (fireEvents) {
            publishChange(InputCreated.create(inputId));
        }

        return inputId;
    }

    @Override
    public String update(Input model) throws ValidationException {
        validateStaticFields(model);
        final InputImpl input = toInputImpl(model);
        final String inputId = input.getId();

        collection.replaceOne(MongoUtils.idEq(inputId), input);
        publishChange(InputUpdated.create(inputId));

        return inputId;
    }

    private void validateStaticFields(Input input) throws ValidationException {
        final Map<String, String> staticFields = input.getStaticFields();
        if (staticFields == null || staticFields.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : staticFields.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            if (key == null || key.isBlank()) {
                throw new ValidationException("Static field key must not be blank");
            }
            if (value == null || value.isBlank()) {
                throw new ValidationException("Static field value for key '" + key + "' must not be blank");
            }
        }

    }

    @Override
    public int destroy(Input model) {
        boolean deleted = mongoUtils.deleteById(model.getId());
        if (deleted) {
            publishChange(InputDeleted.create(model.getId()));
        }
        return deleted ? 1 : 0;
    }

    @Override
    public Input create(String id, Map<String, Object> fields) {
        InputImpl.Builder builder = buildFromMap(fields);
        builder.setId(id);
        return builder.build();
    }

    @Override
    public Input create(Map<String, Object> fields) {
        return buildFromMap(fields).build();
    }

    private InputImpl.Builder buildFromMap(Map<String, Object> fields) {
        final DateTime createdAt = (DateTime) fields.getOrDefault(FIELD_CREATED_AT, Tools.nowUTC());
        final boolean isGlobal = (Boolean) fields.getOrDefault(MessageInput.FIELD_GLOBAL, false);
        final InputImpl.Builder builder = InputImpl.builder()
                .setType((String) fields.get(MessageInput.FIELD_TYPE))
                .setTitle((String) fields.get(MessageInput.FIELD_TITLE))
                .setCreatorUserId((String) fields.get(MessageInput.FIELD_CREATOR_USER_ID))
                .setGlobal(isGlobal)
                .setConfiguration((Map<String, Object>) fields.get(MessageInput.FIELD_CONFIGURATION))
                .setCreatedAt(createdAt);

        final String desiredStateStr = (String) fields.get(MessageInput.FIELD_DESIRED_STATE);
        if (desiredStateStr != null && !desiredStateStr.isBlank()) {
            builder.setPersistedDesiredState(IOState.Type.valueOf(desiredStateStr));
        }

        final String contentPack = (String) fields.get(MessageInput.FIELD_CONTENT_PACK);
        if (contentPack != null && !contentPack.isBlank()) {
            builder.setContentPack(contentPack);
        }

        final List<Map<String, String>> staticFields = (List<Map<String, String>>) fields.get(MessageInput.FIELD_STATIC_FIELDS);
        if (staticFields != null && !staticFields.isEmpty()) {
            builder.setEmbeddedStaticFields(staticFields);
        }

        if (!isGlobal) {
            builder.setNodeId((String) fields.get(MessageInput.FIELD_NODE_ID));
        }

        return builder;
    }

    @Override
    public Input find(String id) throws NotFoundException {
        if (!ObjectId.isValid(id)) {
            throw new NotFoundException("Input id <" + id + "> is invalid!");
        }

        final InputImpl input = mongoUtils.getById(id)
                .orElseThrow(() -> new NotFoundException("Couldn't find input " + id));
        return withEncryptedFields(input);
    }

    @Override
    public Input findForThisNodeOrGlobal(String nodeId, String id) {
        final Bson forThisNodeOrGlobal = or(eq(MessageInput.FIELD_NODE_ID, nodeId), eq(MessageInput.FIELD_GLOBAL, true));
        final Bson query = and(eq(InputImpl.FIELD_ID, new ObjectId(id)), forThisNodeOrGlobal);

        return collection.find(query).first();
    }

    @Override
    public Input findForThisNode(String nodeId, String id) throws NotFoundException, IllegalArgumentException {
        final Bson forThisNodeOrGlobal = or(eq(MessageInput.FIELD_NODE_ID, nodeId), eq(MessageInput.FIELD_GLOBAL, false));
        final Bson query = and(eq(InputImpl.FIELD_ID, new ObjectId(id)), forThisNodeOrGlobal);
        final InputImpl input = collection.find(query).first();
        if (input == null) {
            throw new NotFoundException("Couldn't find input " + id + " on Graylog node " + nodeId);
        } else {
            return input;
        }
    }

    @Override
    public void addExtractor(Input input, Extractor extractor) throws ValidationException  {
        validateExtractor(extractor);
        final Document embeddedDoc = new Document(extractor.getPersistedFields());
        final UpdateResult result = collection.updateOne(
                MongoUtils.idEq(input.getId()),
                Updates.push(InputImpl.EMBEDDED_EXTRACTORS, embeddedDoc)
        );

        if (result.wasAcknowledged()) {
            publishChange(ExtractorCreated.create(input.getId(), extractor.getId()));
        }
    }

    public void validateExtractor(Extractor extractor) throws ValidationException {
        if (StringUtils.isBlank(extractor.getTitle())) {
            throw new ValidationException("Extractor title must not be blank");
        }

        if (extractor.getType() == null) {
            throw new ValidationException("Extractor type must not be blank");
        }

        if (extractor.getCursorStrategy() == null) {
            throw new ValidationException("Extractor cursor strategy must not be blank");
        }

        if (StringUtils.isBlank(extractor.getSourceField())) {
            throw new ValidationException("Extractor source field must not be blank");
        }

        if (StringUtils.isBlank(extractor.getCreatorUserId())) {
            throw new ValidationException("Extractor creator user id must not be blank");
        }

        if (extractor.getExtractorConfig() == null) {
            throw new ValidationException("Extractor config must not be null");
        }
    }

    @Override
    public void updateExtractor(Input input, Extractor extractor) throws ValidationException {
        validateExtractor(extractor);
        final Document embeddedDoc = new Document(extractor.getPersistedFields());

        //First remove the old extractor
        collection.updateOne(
                MongoUtils.idEq(input.getId()),
                MongoUtils.removeEmbedded(InputImpl.EMBEDDED_EXTRACTORS, Extractor.FIELD_ID, extractor.getId())
        );

        collection.updateOne(
                MongoUtils.idEq(input.getId()),
                Updates.push(InputImpl.EMBEDDED_EXTRACTORS, embeddedDoc)
        );

        publishChange(ExtractorUpdated.create(input.getId(), extractor.getId()));
    }

    @Override
    public void addStaticField(Input input, final String key, final String value) throws ValidationException {
        final Document staticFieldDoc = new Document()
                .append(InputImpl.FIELD_STATIC_FIELD_KEY, key)
                .append(InputImpl.FIELD_STATIC_FIELD_VALUE, value);

        final UpdateResult updateResult = collection.updateOne(
                MongoUtils.idEq(input.getId()),
                Updates.push(InputImpl.EMBEDDED_STATIC_FIELDS, staticFieldDoc)
        );

        if (updateResult.wasAcknowledged()) {
            publishChange(InputUpdated.create(input.getId()));
        }
    }

    @Override
    public List<Map.Entry<String, String>> getStaticFields(String inputId) {
        final Document resultDoc = documentCollection.find(
                MongoUtils.idEq(inputId)
        ).projection(Projections.include(InputImpl.EMBEDDED_STATIC_FIELDS)).first();

        return Optional.ofNullable(resultDoc)
                .map(d -> d.getList(InputImpl.EMBEDDED_STATIC_FIELDS, Map.class, List.of()))
                .orElse(Collections.emptyList())
                .stream()
                .map(field -> Map.entry(
                        (String)field.get(InputImpl.FIELD_STATIC_FIELD_KEY),
                        (String)field.get(InputImpl.FIELD_STATIC_FIELD_VALUE)
                ))
                .toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Extractor> getExtractors(String inputId) {
        final Document resultDoc = documentCollection.find(MongoUtils.idEq(inputId))
                .projection(Projections.include(InputImpl.EMBEDDED_EXTRACTORS))
                .first();

        if (resultDoc == null) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<Extractor> listBuilder = ImmutableList.builder();
        final List<Object> list = resultDoc.getList(InputImpl.EMBEDDED_EXTRACTORS, Object.class);
        if (list != null) {
            list.stream()
                    .map(this::toDocument)
                    .map(this::getExtractorFromDoc)
                    .forEach(listBuilder::add);
        }
        return listBuilder.build();
    }

    @Override
    public Extractor getExtractor(final Input input, final String extractorId) throws NotFoundException {
        final Document doc = documentCollection.find(
                        Filters.and(
                                MongoUtils.idEq(input.getId()),
                                Filters.elemMatch(InputImpl.EMBEDDED_EXTRACTORS,
                                        Filters.eq(Extractor.FIELD_ID, extractorId))
                        )
                )
                .projection(Projections.fields(
                        Projections.elemMatch(InputImpl.EMBEDDED_EXTRACTORS,
                                Filters.eq(Extractor.FIELD_ID, extractorId))
                ))
                .first();

        if (doc == null || !doc.containsKey(InputImpl.EMBEDDED_EXTRACTORS)) {
            throw new NotFoundException("Extractor " + extractorId + " not found for input " + input.getId());
        }

        final List<Object> extractors = doc.getList(InputImpl.EMBEDDED_EXTRACTORS, Object.class);
        if (extractors == null || extractors.isEmpty()) {
            throw new NotFoundException("Extractor " + extractorId + " not found for input " + input.getId());
        }

        final Document extractorDoc = toDocument(extractors.getFirst());
        final Extractor extractor = getExtractorFromDoc(extractorDoc);
        if (extractor == null) {
            throw new NotFoundException("Extractor " + extractorId + " not found for input " + input.getId());
        }

        return extractor;
    }

    @SuppressWarnings("unchecked")
    private Document toDocument(Object raw) {
        if (raw instanceof Document doc) {
            return doc;
        }
        if (raw instanceof Map<?, ?> map) {
            return new Document((Map<String, Object>) map);
        }
        throw new IllegalArgumentException("Unsupported value type: " + raw.getClass());
    }

    @SuppressWarnings("unchecked")
    private Extractor getExtractorFromDoc(Document ex) {
        try {
            /* We use json format to describe our test fixtures
                   This format will only return Integer on this place,
                   which can't be converted to long. So I first cast
                   it to Number and eventually to long */
            long order = Optional.ofNullable(ex.get(Extractor.FIELD_ORDER))
                    .map(v -> ((Number) v).longValue())
                    .orElse(0L);

            final UnaryOperator<String> normalizeEnum = s -> s == null ? null : s.toUpperCase(Locale.ENGLISH);

            return extractorFactory.factory(
                    ex.getString(Extractor.FIELD_ID),
                    ex.getString(Extractor.FIELD_TITLE),
                    (int) order,
                    Extractor.CursorStrategy.valueOf(normalizeEnum.apply(ex.getString(Extractor.FIELD_CURSOR_STRATEGY))),
                    Extractor.Type.valueOf(normalizeEnum.apply(ex.getString(Extractor.FIELD_TYPE))),
                    ex.getString(Extractor.FIELD_SOURCE_FIELD),
                    ex.getString(Extractor.FIELD_TARGET_FIELD),
                    (Map<String, Object>) ex.get(Extractor.FIELD_EXTRACTOR_CONFIG),
                    ex.getString(Extractor.FIELD_CREATOR_USER_ID),
                    getConvertersOfExtractor(ex),
                    Extractor.ConditionType.valueOf(normalizeEnum.apply(ex.getString(Extractor.FIELD_CONDITION_TYPE))),
                    ex.getString(Extractor.FIELD_CONDITION_VALUE)
            );
        } catch (Exception e) {
            LOG.error("Cannot build extractor from persisted data. Skipping. Document: {}", ex.toJson(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Converter> getConvertersOfExtractor(Document extractor) {
        final ImmutableList.Builder<Converter> listBuilder = ImmutableList.builder();
        final List<Document> list = extractor.getList(Extractor.FIELD_CONVERTERS, Object.class).stream().map(this::toDocument).toList();

        list.forEach(c -> {
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
        });

        return listBuilder.build();
    }

    @Override
    public void removeExtractor(Input input, String extractorId) {
        UpdateResult updateResult = collection.updateOne(
                MongoUtils.idEq(input.getId()),
                MongoUtils.removeEmbedded(InputImpl.EMBEDDED_EXTRACTORS, Extractor.FIELD_ID, extractorId)
        );

        if (updateResult.wasAcknowledged()) {
            publishChange(ExtractorDeleted.create(input.getId(), extractorId));
        }
    }

    @Override
    public void removeStaticField(Input input, String key) {
        UpdateResult updateResult = collection.updateOne(
                MongoUtils.idEq(input.getId()),
                MongoUtils.removeEmbedded(InputImpl.EMBEDDED_STATIC_FIELDS, InputImpl.FIELD_STATIC_FIELD_KEY, key)
        );

        if (updateResult.wasAcknowledged()) {
            publishChange(InputUpdated.create(input.getId()));
        }
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
        input.setDesiredState(io.getDesiredState());

        if (io.isGlobal()) {
            input.setGlobal(true);
        }

        // Add static fields.
        input.addStaticFields(io.getStaticFields());

        return input;
    }

    @Override
    public long totalCount() {
        return collection.countDocuments();
    }

    @Override
    public long globalCount() {
        return collection.countDocuments(eq(MessageInput.FIELD_GLOBAL, true));
    }

    @Override
    public long localCount() {
        return collection.countDocuments(eq(MessageInput.FIELD_GLOBAL, false));
    }

    @Override
    public Map<String, Long> totalCountByType() {
        final Map<String, Long> inputCountByType = new HashMap<>();

        final List<Bson> pipeline = List.of(
                Aggregates.group("$" + MessageInput.FIELD_TYPE, Accumulators.sum("count", 1)),
                Aggregates.sort(Sorts.ascending(MessageInput.FIELD_TYPE))
        );

        collection.aggregate(pipeline, Document.class)
                .forEach(doc -> {
                    final String type = doc.getString(MessageInput.FIELD_TYPE);
                    if (type != null) {
                        final long count = doc.get("count", Long.class);
                        inputCountByType.put(type, count);
                    }
                });

        return inputCountByType;
    }

    @Override
    public long localCountForNode(String nodeId) {
        return collection.countDocuments(and(
                eq(MessageInput.FIELD_NODE_ID, nodeId),
                eq(MessageInput.FIELD_GLOBAL, false)
        ));
    }

    @Override
    public long totalCountForNode(String nodeId) {
        return collection.countDocuments(or(
                eq(MessageInput.FIELD_NODE_ID, nodeId),
                eq(MessageInput.FIELD_GLOBAL, true)
        ));
    }

    @Override
    public long totalExtractorCount() {
        final AtomicLong extractorsCount = new AtomicLong();
        documentCollection.find(Filters.exists(InputImpl.EMBEDDED_EXTRACTORS))
                .projection(Projections.include(InputImpl.EMBEDDED_EXTRACTORS))
                .forEach(docs -> {
                    List<?> extractors = docs.getList(InputImpl.EMBEDDED_EXTRACTORS, Object.class);
                    if (extractors != null) {
                        extractorsCount.addAndGet(extractors.size());
                    }
                });
        return extractorsCount.get();
    }

    @Override
    public Map<Extractor.Type, Long> totalExtractorCountByType() {
        final Map<Extractor.Type, Long> extractorsCountByType = new HashMap<>();

        documentCollection.find(Filters.exists(InputImpl.EMBEDDED_EXTRACTORS))
                .projection(Projections.include(InputImpl.EMBEDDED_EXTRACTORS))
                .forEach(doc -> {
                    List<Document> extractors = doc.getList(InputImpl.EMBEDDED_EXTRACTORS, Document.class);
                    if (extractors == null) return;

                    for (Document extractorDoc : extractors) {
                        final String typeStr = extractorDoc.getString(Extractor.FIELD_TYPE);
                        final Extractor.Type type = Extractor.Type.fuzzyValueOf(typeStr);

                        if (type != null) {
                            extractorsCountByType.merge(type, 1L, Long::sum);
                        }
                    }
                });

        return extractorsCountByType;
    }

    private void publishChange(Object event) {
        this.clusterEventBus.post(event);
    }

    private Set<String> getEncryptedFields(String type) {
        return messageInputFactory.getConfig(type)
                .map(EncryptedInputConfigs::getEncryptedFields)
                .orElse(Set.of());
    }

    private InputImpl withEncryptedFields(InputImpl input) {
        if (input == null) {
            return null;
        }
        final Set<String> encryptedFields = getEncryptedFields(input.getType());
        if (encryptedFields.isEmpty()) {
            return input;
        }
        final Map<String, Object> originalConfig = input.getConfiguration();
        if (originalConfig == null || originalConfig.isEmpty()) {
            return input;
        }

        boolean modified = false;
        final Map<String, Object> newConfig = new HashMap<>(originalConfig);
        for (String field : encryptedFields) {
            final Object raw = newConfig.get(field);
            if (raw != null && !(raw instanceof EncryptedValue)) {
                try {
                    final EncryptedValue ev = objectMapper.convertValue(raw, EncryptedValue.class);
                    newConfig.put(field, ev);
                    modified = true;
                } catch (IllegalArgumentException e) {
                    LOG.warn("Failed to convert field '{}' to EncryptedValue for input '{}': {}", field, input.getId(), e.getMessage());
                }
            }
        }
        return modified ? input.toBuilder().setConfiguration(newConfig).build() : input;
    }

    @Override
    public void persistDesiredState(Input input, IOState.Type desiredState) throws ValidationException {
        final Input updatedInput = input.withDesiredState(desiredState);
        saveWithoutEvents(updatedInput);
    }
}
