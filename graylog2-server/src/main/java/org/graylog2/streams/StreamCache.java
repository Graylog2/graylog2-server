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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.streams.events.StreamsChangedEvent;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.database.entities.ScopedEntity.FIELD_SCOPE;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.plugin.streams.Stream.DEFAULT_STREAM_ID;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Singleton cache for stream metadata (titles and system stream IDs).
 * <p>
 * Registers with {@link EventBus} for cache invalidation on stream changes.
 * Singleton to not leak instances via the EventBus.
 */
@Singleton
public class StreamCache {

    private static final String SYSTEM_STREAM_IDS_KEY = "systemStreamIds";

    private final LoadingCache<String, String> titleCache;
    private final LoadingCache<String, Set<String>> systemStreamIdsCache;

    @Inject
    public StreamCache(MongoCollections mongoCollections, EventBus eventBus) {
        final MongoCollection<StreamDTO> collection =
                mongoCollections.collection(StreamServiceImpl.COLLECTION_NAME, StreamDTO.class);

        this.titleCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(10))
                .build(new CacheLoader<>() {
                    @Nonnull
                    @Override
                    public String load(@Nonnull String streamId) throws NotFoundException {
                        try (var s = stream(collection.find(idEq(streamId)))) {
                            return s.map(StreamDTO::title)
                                    .findFirst()
                                    .orElseThrow(() -> new NotFoundException(f("Couldn't find stream %s", streamId)));
                        }
                    }
                });

        this.systemStreamIdsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(Duration.ofSeconds(10))
                .build(new CacheLoader<>() {
                    @Nonnull
                    @Override
                    public Set<String> load(@Nonnull String ignored) {
                        try (var s = stream(collection.find(eq(FIELD_SCOPE, ImmutableSystemScope.NAME)))) {
                            return s.map(StreamDTO::id).collect(Collectors.toUnmodifiableSet());
                        }
                    }
                });

        eventBus.register(this);
    }

    @Subscribe
    public void handleStreamsChanged(StreamsChangedEvent event) {
        event.streamIds().forEach(titleCache::invalidate);
        systemStreamIdsCache.invalidateAll();
    }

    @Nullable
    public String streamTitleFromCache(String streamId) {
        try {
            return titleCache.get(streamId);
        } catch (Exception e) {
            return null;
        }
    }

    public Set<String> getSystemStreamIds(boolean includeDefaultStream) {
        final Set<String> ids = systemStreamIdsCache.getUnchecked(SYSTEM_STREAM_IDS_KEY);
        return includeDefaultStream ? Sets.union(ids, Set.of(DEFAULT_STREAM_ID)) : ids;
    }

    public void invalidateTitle(String streamId) {
        titleCache.invalidate(streamId);
    }
}
