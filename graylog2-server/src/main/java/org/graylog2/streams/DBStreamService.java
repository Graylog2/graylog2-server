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

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.MongoIndexSet;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.addEachToSet;
import static com.mongodb.client.model.Updates.pull;
import static com.mongodb.client.model.Updates.set;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.idsIn;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.streams.StreamDTO.FIELD_CATEGORIES;
import static org.graylog2.streams.StreamDTO.FIELD_CONTENT_PACK;
import static org.graylog2.streams.StreamDTO.FIELD_CREATED_AT;
import static org.graylog2.streams.StreamDTO.FIELD_CREATOR_USER_ID;
import static org.graylog2.streams.StreamDTO.FIELD_DESCRIPTION;
import static org.graylog2.streams.StreamDTO.FIELD_DISABLED;
import static org.graylog2.streams.StreamDTO.FIELD_INDEX_SET_ID;
import static org.graylog2.streams.StreamDTO.FIELD_MATCHING_TYPE;
import static org.graylog2.streams.StreamDTO.FIELD_OUTPUTS;
import static org.graylog2.streams.StreamDTO.FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM;
import static org.graylog2.streams.StreamDTO.FIELD_TITLE;

public class DBStreamService implements StreamService {
    private static final Logger LOG = LoggerFactory.getLogger(DBStreamService.class);
    private static final String COLLECTION_NAME = "streams";
    private final MongoCollection<StreamDTO> collection;
    private final MongoUtils<StreamDTO> mongoUtils;
    private final StreamRuleService streamRuleService;
    private final OutputService outputService;
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory indexSetFactory;
    private final EntityOwnershipService entityOwnershipService;
    private final ClusterEventBus clusterEventBus;
    private final Set<StreamDeletionGuard> streamDeletionGuards;
    private final LoadingCache<String, String> streamTitleCache;

    @Inject
    public DBStreamService(MongoCollections mongoCollections,
                           StreamRuleService streamRuleService,
                           OutputService outputService,
                           IndexSetService indexSetService,
                           MongoIndexSet.Factory indexSetFactory,
                           EntityOwnershipService entityOwnershipService,
                           ClusterEventBus clusterEventBus,
                           Set<StreamDeletionGuard> streamDeletionGuards) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, StreamDTO.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.streamRuleService = streamRuleService;
        this.outputService = outputService;
        this.indexSetService = indexSetService;
        this.indexSetFactory = indexSetFactory;
        this.entityOwnershipService = entityOwnershipService;
        this.clusterEventBus = clusterEventBus;
        this.streamDeletionGuards = streamDeletionGuards;

        final CacheLoader<String, String> streamTitleLoader = new CacheLoader<>() {
            @Nonnull
            @Override
            public String load(@Nonnull String streamId) throws NotFoundException {
                String title = loadStreamTitles(List.of(streamId)).get(streamId);
                if (title != null) {
                    return title;
                } else {
                    throw new NotFoundException(f("Couldn't find stream %s", streamId));
                }
            }
        };

        this.streamTitleCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .build(streamTitleLoader);

    }

    @Nullable
    private IndexSet getIndexSet(String id) {
        if (isNullOrEmpty(id)) {
            return null;
        }
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(id);
        return indexSetConfig.flatMap(c -> Optional.of(indexSetFactory.create(c))).orElse(null);
    }

    @Override
    public Stream create(Map<String, Object> fields) {
        return new StreamImpl(fields, getIndexSet((String) fields.get(FIELD_INDEX_SET_ID)));
    }

    @Override
    public Stream create(CreateStreamRequest cr, String userId) {
        Map<String, Object> streamData = Maps.newHashMap();
        streamData.put(FIELD_TITLE, cr.title().strip());
        streamData.put(FIELD_DESCRIPTION, cr.description());
        streamData.put(FIELD_CREATOR_USER_ID, userId);
        streamData.put(FIELD_CREATED_AT, Tools.nowUTC());
        streamData.put(FIELD_CONTENT_PACK, cr.contentPack());
        streamData.put(FIELD_MATCHING_TYPE, cr.matchingType().toString());
        streamData.put(FIELD_DISABLED, false);
        streamData.put(FIELD_REMOVE_MATCHES_FROM_DEFAULT_STREAM, cr.removeMatchesFromDefaultStream());
        streamData.put(FIELD_INDEX_SET_ID, cr.indexSetId());

        return create(streamData);
    }

    @Override
    public Stream load(String id) throws NotFoundException {
        try {
            final StreamDTO dto = mongoUtils.getById(id)
                    .orElseThrow(() -> new NotFoundException("Stream <" + id + "> not found!"));
            return fromDTO(dto);
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }
    }

    @Override
    public List<Stream> loadAllEnabled() {
        return loadAllByQuery(eq(FIELD_DISABLED, false));
    }

    @Override
    public List<Stream> loadAllByTitle(String title) {
        return loadAllByQuery(eq(FIELD_TITLE, title));
    }

    @Override
    public Map<String, String> loadStreamTitles(Collection<String> streamIds) {
        if (streamIds.isEmpty()) {
            return Map.of();
        }

        try (var stream = stream(collection.find(stringIdsIn(streamIds)))) {
            return stream.collect(Collectors.toMap(MongoEntity::id, StreamDTO::title));
        }
    }

    @Override
    @Nullable
    public String streamTitleFromCache(String streamId) {
        try {
            return streamTitleCache.get(streamId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Stream> loadAll() {
        return loadAllByQuery(new Document());
    }

    @Override
    public java.util.stream.Stream<String> streamAllIds() {
        return stream(collection.find()).map(StreamDTO::id);
    }

    @Override
    public java.util.stream.Stream<StreamDTO> streamAllDTOs() {
        return stream(collection.find());
    }

    @Override
    public java.util.stream.Stream<StreamDTO> streamDTOByIds(Collection<String> streamIds) {
        return stream(collection.find(stringIdsIn(streamIds)));
    }

    private List<Stream> loadAllByQuery(Bson query) {
        final List<StreamDTO> results;
        try (var stream = stream(collection.find(query))) {
            results = stream.toList();
        }
        final List<String> streamIds = results.stream()
                .map(StreamDTO::id)
                .toList();
        final Map<String, List<StreamRule>> allStreamRules = streamRuleService.loadForStreamIds(streamIds);

        final ImmutableList.Builder<Stream> streams = ImmutableList.builder();

        final Map<String, IndexSet> indexSets = indexSetsForStreams(results);

        final Set<String> outputIds = results.stream()
                .map(StreamDTO::outputs)
                .filter(Objects::nonNull)
                .flatMap(outputs -> outputs.stream().map(ObjectId::toHexString))
                .collect(Collectors.toSet());

        final Map<String, Output> outputsById = outputService.loadByIds(outputIds)
                .stream()
                .collect(Collectors.toMap(Output::getId, Function.identity()));


        for (StreamDTO dto: results) {
            final String id = dto.id();
            final List<StreamRule> streamRules = allStreamRules.getOrDefault(id, Collections.emptyList());
            LOG.debug("Found {} rules for stream <{}>", streamRules.size(), id);

            final Set<Output> outputs= new HashSet<>();
            if (dto.outputs() != null) {
                outputs.addAll(dto.outputs()
                        .stream()
                        .map(ObjectId::toHexString)
                        .map(outputId -> {
                            final Output output = outputsById.get(outputId);
                            if (output == null) {
                                final String streamTitle = Strings.nullToEmpty(dto.title());
                                LOG.warn("Stream \"" + streamTitle + "\" <" + id + "> references missing output <" + outputId + "> - ignoring output.");
                            }
                            return output;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
            }

            streams.add(new StreamImpl(new ObjectId(id), StreamDTO.toMap(dto), streamRules, outputs, indexSets.get(dto.indexSetId())));
        }

        return streams.build();
    }

    private Map<String, IndexSet> indexSetsForStreams(List<StreamDTO> streams) {
        final Set<String> indexSetIds = streams.stream()
                .map(StreamDTO::indexSetId)
                .filter(s -> !isNullOrEmpty(s))
                .collect(Collectors.toSet());
        return indexSetService.findByIds(indexSetIds)
                .stream()
                .collect(Collectors.toMap(IndexSetConfig::id, indexSetFactory::create));
    }

    @Override
    public Set<Stream> loadByIds(Collection<String> streamIds) {
        final Set<ObjectId> objectIds = streamIds.stream()
                .map(ObjectId::new)
                .collect(Collectors.toSet());

        return ImmutableSet.copyOf(loadAllByQuery(idsIn(objectIds)));
    }

    @Override
    public Set<String> mapCategoriesToIds(Collection<String> categories) {
        try (var stream = stream(collection.find(in(FIELD_CATEGORIES, categories)))) {
            return stream.map(StreamDTO::id).collect(Collectors.toSet());
        }
    }

    @Override
    public Set<String> indexSetIdsByIds(Collection<String> streamIds) {
        Set<String> dataStreamIds = streamIds.stream()
                .filter(s -> s.startsWith(Stream.DATASTREAM_PREFIX))
                .collect(Collectors.toSet());

        final Set<ObjectId> objectIds = streamIds.stream()
                .filter(s -> !s.startsWith(Stream.DATASTREAM_PREFIX))
                .map(ObjectId::new)
                .collect(Collectors.toSet());
        Set<String> indexSets = new HashSet<>(dataStreamIds);
        try (var stream = stream(collection.find(idsIn(objectIds)))) {
            indexSets.addAll(stream.map(StreamDTO::indexSetId).collect(Collectors.toSet()));
        }
        return indexSets;
    }

    protected Set<Output> loadOutputsForRawStream(StreamDTO stream) {
        Collection<ObjectId> outputIds = stream.outputs();

        Set<Output> result = new HashSet<>();
        if (outputIds != null) {
            for (ObjectId outputId : outputIds) {
                try {
                    result.add(outputService.load(outputId.toHexString()));
                } catch (NotFoundException e) {
                    LOG.warn("Non-existing output <{}> referenced from stream <{}>!", outputId.toHexString(), stream.id());
                }
            }
        }

        return result;
    }

    @Override
    public long count() {
        return collection.countDocuments();
    }

    @Override
    public void destroy(Stream stream) throws NotFoundException, StreamGuardException {
        checkDeletionGuards(stream.getId());

        for (StreamRule streamRule : streamRuleService.loadForStream(stream)) {
            streamRuleService.destroy(streamRule);
        }

        final String streamId = stream.getId();
        // we need to remove notifications referencing this stream. This happens in the DeletedStreamNotificationListener
        // triggered by the StreamDeletedEvent below.
        collection.deleteOne(idEq(stream.getId()));

        clusterEventBus.post(StreamsChangedEvent.create(streamId));
        clusterEventBus.post(StreamDeletedEvent.create(streamId));
        entityOwnershipService.unregisterStream(streamId);
    }

    private void checkDeletionGuards(String streamId) throws StreamGuardException {
        for (StreamDeletionGuard guard : streamDeletionGuards) {
            guard.checkGuard(streamId);
        }
    }

    @Override
    public void pause(Stream stream) throws ValidationException {
        collection.updateOne(idEq(stream.getId()), set(FIELD_DISABLED, true));
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @Override
    public void resume(Stream stream) throws ValidationException {
        collection.updateOne(idEq(stream.getId()), set(FIELD_DISABLED, false));
        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @Override
    public void addOutput(Stream stream, Output output) {
        addOutputs(new ObjectId(stream.getId()), List.of(new ObjectId(output.getId())));
    }

    @Override
    public void addOutputs(ObjectId streamId, Collection<ObjectId> outputIds) {
        final List<ObjectId> outputIdList = new ArrayList<>(outputIds);

        collection.updateOne(
                idEq(streamId),
                addEachToSet(FIELD_OUTPUTS, outputIdList)
        );
        clusterEventBus.post(StreamsChangedEvent.create(streamId.toHexString()));
    }

    @Override
    public void removeOutput(Stream stream, Output output) {
        collection.updateOne(
                idEq(stream.getId()),
                pull(FIELD_OUTPUTS, new ObjectId(output.getId()))
        );

        clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
    }

    @Override
    public void removeOutputFromAllStreams(Output output) {
        ObjectId outputId = new ObjectId(output.getId());
        Bson hasOutput = eq(FIELD_OUTPUTS, outputId);
        Bson pullOutput = pull(FIELD_OUTPUTS, outputId);

        // Collect streams that will change before updating them because we don't get the list of changed streams
        // from the upsert call.
        final ImmutableSet<String> updatedStreams;
        try (var stream = stream(collection.find(hasOutput))) {
            updatedStreams = stream
                    .map(StreamDTO::id)
                    .filter(Objects::nonNull)
                    .collect(ImmutableSet.toImmutableSet());
        }

        collection.updateMany(hasOutput, pullOutput);

        clusterEventBus.post(StreamsChangedEvent.create(updatedStreams));
    }

    @Override
    public List<Stream> loadAllWithIndexSet(String indexSetId) {
        return loadAllByQuery(eq(FIELD_INDEX_SET_ID, indexSetId));
    }

    @Override
    public void addToIndexSet(String indexSetId, Collection<String> streamIds) {
        final UpdateResult update = collection.updateMany(stringIdsIn(streamIds), set(FIELD_INDEX_SET_ID, indexSetId));

        if (update.getModifiedCount() < streamIds.stream().distinct().count()) {
            throw new IllegalStateException("Assigning streams " + streamIds + " to index set <" + indexSetId + "> failed!");
        }
    }

    @Override
    public String save(Stream stream) throws ValidationException {
        final String id = stream.getId();
        final StreamDTO dto = toDTO(stream);
        collection.replaceOne(idEq(id), dto, new ReplaceOptions().upsert(true));

        return id;
    }

    @Override
    public String saveWithRulesAndOwnership(Stream stream, Collection<StreamRule> streamRules, User user) throws ValidationException {
        final String savedStreamId = save(stream);
        final Set<StreamRule> rules = streamRules.stream()
                .map(rule -> streamRuleService.copy(savedStreamId, rule))
                .collect(Collectors.toSet());
        streamRuleService.save(rules);

        entityOwnershipService.registerNewStream(savedStreamId, user);
        clusterEventBus.post(StreamsChangedEvent.create(savedStreamId));

        return savedStreamId;
    }

    @Override
    public List<String> streamTitlesForIndexSet(final String indexSetId) {
        List<String> result = new LinkedList<>();
        try (var stream = stream(collection.find(eq(FIELD_INDEX_SET_ID, indexSetId)))) {
            stream.map(StreamDTO::title).forEach(result::add);
        }
        return result;
    }

    @Override
    public Map<String, Long> streamRuleCountByStream() {
        final ImmutableMap.Builder<String, Long> streamRules = ImmutableMap.builder();
        try (var streamIds = streamAllIds()) {
            streamIds.forEach(streamId -> {
                streamRules.put(streamId, streamRuleService.streamRuleCount(streamId));
            });
        }

        return streamRules.build();
    }

    private Stream fromDTO(StreamDTO dto) {
        final List<StreamRule> streamRules = streamRuleService.loadForStreamId(dto.id());
        final Set<Output> outputs = loadOutputsForRawStream(dto);
        final IndexSet indexSet = getIndexSet(dto.indexSetId());

        final Map<String, Object> fields = StreamDTO.toMap(dto);
        return new StreamImpl(new ObjectId(dto.id()), fields, streamRules, outputs, indexSet);
    }

    private StreamDTO toDTO(Stream stream) {
        final Date createdAt;
        // TODO: Clean this up, created_at comes in different forms from different places.
        final Object createdDate = stream.getFields().get(FIELD_CREATED_AT);
        if (createdDate instanceof String dateString) {
            createdAt = Date.from(Instant.parse(dateString));
        } else if (createdDate instanceof DateTime dateTime) {
            createdAt = dateTime.toDate();
        } else if (createdDate instanceof Date date) {
            createdAt = date;
        } else {
            throw new IllegalArgumentException(f("Unexpected type of %s: %s", FIELD_CREATED_AT, createdDate));
        }
        final String createdBy = (String) stream.getFields().get(FIELD_CREATOR_USER_ID);
        final StreamDTO.Builder dtoBuilder = StreamDTO.builder()
                .id(stream.getId())
                .creatorUserId(createdBy)
                .matchingType(stream.getMatchingType().toString())
                .description(stream.getDescription())
                .createdAt(createdAt)
                .disabled(stream.getDisabled())
                .title(stream.getTitle())
                .contentPack(stream.getContentPack())
                .isDefault(stream.isDefaultStream())
                .removeMatchesFromDefaultStream(stream.getRemoveMatchesFromDefaultStream())
                .indexSetId(stream.getIndexSetId());
        if (stream.getOutputs() != null) {
            dtoBuilder.outputs(stream.getOutputs().stream().map(o -> new ObjectId(o.getId())).toList());
        }
        if (stream.getCategories() != null) {
            dtoBuilder.categories(stream.getCategories());
        }
        // Intentionally do not add any StreamRules to the DTO. These are not saved in the streams collection and are
        // linked from the streamrules collection when queried.
        return dtoBuilder.build();
    }
}
