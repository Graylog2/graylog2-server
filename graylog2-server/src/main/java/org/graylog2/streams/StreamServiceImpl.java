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
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoEntity;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.entities.ImmutableSystemScope;
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
import org.graylog2.rest.models.streams.requests.UpdateStreamRequest;
import org.graylog2.rest.resources.streams.requests.CreateStreamRequest;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.graylog2.streams.events.StreamRenamedEvent;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Updates.addEachToSet;
import static com.mongodb.client.model.Updates.pull;
import static com.mongodb.client.model.Updates.set;
import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.idsIn;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.graylog2.shared.utilities.StringUtils.f;
import static org.graylog2.streams.StreamImpl.FIELD_CATEGORIES;
import static org.graylog2.streams.StreamImpl.FIELD_DISABLED;
import static org.graylog2.streams.StreamImpl.FIELD_INDEX_SET_ID;
import static org.graylog2.streams.StreamImpl.FIELD_OUTPUTS;
import static org.graylog2.streams.StreamImpl.FIELD_TITLE;

public class StreamServiceImpl implements StreamService {
    private static final Logger LOG = LoggerFactory.getLogger(StreamServiceImpl.class);
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
    private final EntityScopeService scopeService;
    private final LoadingCache<String, String> streamTitleCache;
    private Set<String> systemStreamIds;

    @Inject
    public StreamServiceImpl(MongoCollections mongoCollections,
                             StreamRuleService streamRuleService,
                             OutputService outputService,
                             IndexSetService indexSetService,
                             MongoIndexSet.Factory indexSetFactory,
                             EntityOwnershipService entityOwnershipService,
                             ClusterEventBus clusterEventBus,
                             Set<StreamDeletionGuard> streamDeletionGuards,
                             EntityScopeService scopeService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, StreamDTO.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.streamRuleService = streamRuleService;
        this.outputService = outputService;
        this.indexSetService = indexSetService;
        this.indexSetFactory = indexSetFactory;
        this.entityOwnershipService = entityOwnershipService;
        this.clusterEventBus = clusterEventBus;
        this.streamDeletionGuards = streamDeletionGuards;
        this.scopeService = scopeService;

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
    public Stream create(CreateStreamRequest cr, String userId) {
        return StreamImpl.builder()
                .id(new ObjectId().toHexString())
                .title(cr.title().strip())
                .description(cr.description())
                .creatorUserId(userId)
                .createdAt(Tools.nowUTC())
                .contentPack(cr.contentPack())
                .matchingType(cr.matchingType())
                .disabled(true)
                .removeMatchesFromDefaultStream(cr.removeMatchesFromDefaultStream())
                .indexSetId(cr.indexSetId())
                .indexSet(getIndexSet(cr.indexSetId()))
                .build();
    }

    @Override
    public Stream load(String id) throws NotFoundException {
        List<Stream> singleStream = loadAllByQuery(idEq(id));
        if (singleStream.isEmpty()) {
            throw new NotFoundException("Stream <" + id + "> not found!");
        }
        return singleStream.get(0);
    }

    @Override
    public List<Stream> loadAllEnabled() {
        return loadAllByQuery(eq(FIELD_DISABLED, false));
    }

    @Override
    public List<Stream> loadSystemStreams(boolean includeDefaultStream) {
        Bson filter = eq(FIELD_SCOPE, ImmutableSystemScope.NAME);
        if (includeDefaultStream) {
            filter = or(
                    filter,
                    idEq(DEFAULT_STREAM_ID)
            );
        }
        return loadAllByQuery(filter);
    }

    @Override
    public Set<String> getSystemStreamIds(boolean includeDefaultStream) {
        if (systemStreamIds == null) {
            try (var stream = streamAllDTOs()) {
                systemStreamIds = stream
                        .filter(s -> ImmutableSystemScope.NAME.equals(s.getScope()))
                        .map(Stream::getId)
                        .collect(Collectors.toSet());
            }
        }
        return includeDefaultStream ? java.util.stream.Stream.concat(systemStreamIds.stream(),
                java.util.stream.Stream.of(DEFAULT_STREAM_ID)).collect(Collectors.toSet()) : systemStreamIds;
    }

    @Override
    public boolean isSystemStream(String id) {
        final StreamDTO dto = mongoUtils.getById(id).orElse(null);
        if (dto == null) {
            return false;
        }
        return ImmutableSystemScope.NAME.equals(dto.scope());
    }

    @Override
    public boolean isEditable(String id) {
        final StreamDTO dto = mongoUtils.getById(id).orElse(null);
        if (dto == null) {
            return true;
        }
        return scopeService.isMutable(dto);
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
    @MustBeClosed
    public java.util.stream.Stream<String> streamAllIds() {
        return stream(collection.find()).map(StreamDTO::id);
    }

    @Override
    @MustBeClosed
    public java.util.stream.Stream<Stream> streamAllDTOs() {
        return stream(collection.find())
                .map(dto -> dto.toBuilder().isEditable(scopeService.scopeIsMutable(dto.scope())).build())
                .map(StreamImpl::fromDTO);
    }

    @Override
    @MustBeClosed
    public java.util.stream.Stream<Stream> streamDTOByIds(Collection<String> streamIds) {
        return stream(collection.find(stringIdsIn(streamIds)))
                .map(dto -> dto.toBuilder().isEditable(scopeService.scopeIsMutable(dto.scope())).build())
                .map(StreamImpl::fromDTO);
    }

    private List<Stream> loadAllByQuery(Bson query) {
        final List<StreamImpl> results;
        try (var stream = stream(collection.find(query))) {
            results = stream
                    .map(dto -> dto.toBuilder().isEditable(scopeService.scopeIsMutable(dto.scope())).build())
                    .map(StreamImpl::fromDTO)
                    .toList();
        }
        final List<String> streamIds = results.stream()
                .map(StreamImpl::id)
                .toList();
        final Map<String, List<StreamRule>> allStreamRules = streamRuleService.loadForStreamIds(streamIds);

        final ImmutableList.Builder<Stream> streams = ImmutableList.builder();

        final Map<String, IndexSet> indexSets = indexSetsForStreams(results);

        final Set<String> outputIds = results.stream()
                .map(StreamImpl::outputIds)
                .filter(Objects::nonNull)
                .flatMap(outputs -> outputs.stream().map(ObjectId::toHexString))
                .collect(Collectors.toSet());

        final Map<String, Output> outputsById = outputService.loadByIds(outputIds)
                .stream()
                .collect(Collectors.toMap(Output::getId, Function.identity()));


        for (StreamImpl dto : results) {
            final String id = dto.id();
            final List<StreamRule> streamRules = allStreamRules.getOrDefault(id, Collections.emptyList());
            LOG.debug("Found {} rules for stream <{}>", streamRules.size(), id);

            final Set<Output> outputs = new HashSet<>();
            if (dto.outputIds() != null) {
                outputs.addAll(dto.outputIds()
                        .stream()
                        .map(ObjectId::toHexString)
                        .map(outputId -> {
                            final Output output = outputsById.get(outputId);
                            if (output == null) {
                                final String streamTitle = Strings.nullToEmpty(dto.title());
                                LOG.warn("Stream \"{}\" <{}> references missing output <{}> - ignoring output.", streamTitle, id, outputId);
                            }
                            return output;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()));
            }

            streams.add(dto.toBuilder()
                    .rules(streamRules)
                    .outputObjects(outputs)
                    .indexSet(indexSets.get(dto.indexSetId()))
                    .build());
        }

        return streams.build();
    }

    private Map<String, IndexSet> indexSetsForStreams(List<StreamImpl> streams) {
        final Set<String> indexSetIds = streams.stream()
                .map(StreamImpl::indexSetId)
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
    @MustBeClosed
    public java.util.stream.Stream<String> mapCategoriesToIds(Collection<String> categories) {
        return stream(collection.find(in(FIELD_CATEGORIES, categories))).map(StreamDTO::id);
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
        clusterEventBus.post(new StreamDeletedEvent(streamId, stream.getTitle()));
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
        final StreamImpl streamImpl = (StreamImpl) stream;
        collection.replaceOne(idEq(Objects.requireNonNull(streamImpl.id())), streamImpl.toDTO(), new ReplaceOptions().upsert(true));

        return streamImpl.id();
    }

    @Override
    public Stream update(String streamId, UpdateStreamRequest request) throws NotFoundException, ValidationException {
        final StreamImpl existingStream = (StreamImpl) load(streamId);
        final StreamImpl.Builder streamBuilder = existingStream.toBuilder();

        StreamRenamedEvent streamRenamedEvent = null;
        if (!Strings.isNullOrEmpty(request.title())) {
            String newTitle = request.title().strip();
            if (!newTitle.equals(existingStream.getTitle())) {
                streamRenamedEvent = new StreamRenamedEvent(streamId, existingStream.getTitle(), newTitle);
                streamBuilder.title(newTitle);
            }
        }

        streamBuilder.description(request.description());

        if (request.matchingType() != null) {
            try {
                streamBuilder.matchingType(Stream.MatchingType.valueOf(request.matchingType()));
            } catch (IllegalArgumentException e) {
                throw new ValidationException("Invalid matching type '" + request.matchingType()
                        + "' specified. Should be one of: " + Arrays.toString(Stream.MatchingType.values()));
            }
        }

        final Boolean removeMatchesFromDefaultStream = request.removeMatchesFromDefaultStream();
        if (removeMatchesFromDefaultStream != null) {
            streamBuilder.removeMatchesFromDefaultStream(removeMatchesFromDefaultStream);
        }

        // Apparently we are sending partial resources sometimes so do not overwrite the index set
        // id if it's null/empty in the update request.
        if (!Strings.isNullOrEmpty(request.indexSetId())) {
            final String requestedIndexSet = request.indexSetId();
            final IndexSetConfig indexSetConfig = indexSetService.get(requestedIndexSet)
                    .orElseThrow(() -> new ValidationException("Index set with ID <" + requestedIndexSet + "> does not exist!"));

            if (!indexSetConfig.isWritable()) {
                throw new ValidationException("Assigned index set must be writable!");
            }
            if (!indexSetConfig.isRegularIndex()) {
                throw new ValidationException("Assigned index set is not usable");
            }
            streamBuilder.indexSetId(requestedIndexSet);
        }

        final Stream updatedStream = streamBuilder.build();
        save(updatedStream);
        if (streamRenamedEvent != null) {
            clusterEventBus.post(streamRenamedEvent);
        }
        return updatedStream;
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
        try (var stream = stream(collection.find(eq(FIELD_INDEX_SET_ID, indexSetId)))) {
            return stream.map(StreamDTO::title).toList();
        }
    }

    @Override
    public Map<String, Long> streamRuleCountByStream() {
        try (var streamIds = streamAllIds()) {
            return streamIds.collect(Collectors.toUnmodifiableMap(Function.identity(), streamRuleService::streamRuleCount));
        }
    }
}
