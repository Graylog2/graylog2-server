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
package org.graylog2.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Ints;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import jakarta.inject.Inject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.UnsafeClassLoadingAttemptException;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterConfigServiceImpl implements ClusterConfigService {
    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_config";
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigServiceImpl.class);
    private final MongoCollection<ClusterConfig> collection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final RestrictedChainingClassLoader chainingClassLoader;
    private final EventBus clusterEventBus;

    @Inject
    public ClusterConfigServiceImpl(final MongoJackObjectMapperProvider mapperProvider,
                                    final MongoConnection mongoConnection,
                                    final NodeId nodeId,
                                    final RestrictedChainingClassLoader chainingClassLoader,
                                    final ClusterEventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.collection = prepareCollection(mongoConnection, mapperProvider);
        this.objectMapper = checkNotNull(mapperProvider.get());
        this.chainingClassLoader = chainingClassLoader;
        this.clusterEventBus = checkNotNull(clusterEventBus);
    }

    @VisibleForTesting
    static MongoCollection<ClusterConfig> prepareCollection(final MongoConnection mongoConnection,
                                                            MongoJackObjectMapperProvider mapperProvider) {
        final MongoCollection<ClusterConfig> collection =
                new MongoCollections(mapperProvider, mongoConnection).collection(COLLECTION_NAME, ClusterConfig.class)
                        .withWriteConcern(WriteConcern.JOURNALED);
        collection.createIndex(Indexes.ascending("type"), new IndexOptions().name("unique_type").unique(true));
        return collection;
    }

    @Override
    public <T> T extractPayload(Object payload, Class<T> type) {
        try {
            return objectMapper.convertValue(payload, type);
        } catch (IllegalArgumentException e) {
            LOG.error("Error while deserializing payload", e);
            return null;
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        ClusterConfig config = findClusterConfig(key);

        if (config == null) {
            LOG.debug("Couldn't find cluster config of type {}", key);
            return null;
        }

        T result = extractPayload(config.payload(), type);
        if (result == null) {
            LOG.error("Couldn't extract payload from cluster config (type: {})", key);
        }

        return result;
    }

    private ClusterConfig findClusterConfig(String key) {
        return collection.find(Filters.eq("type", key)).first();
    }

    @Override
    public <T> T get(Class<T> type) {
        return get(type.getCanonicalName(), type);
    }

    @Override
    public ClusterConfig getRaw(Class<?> type) {
        return findClusterConfig(type.getCanonicalName());
    }

    @Override
    public <T> T getOrDefault(Class<T> type, T defaultValue) {
        return firstNonNull(get(type), defaultValue);
    }

    @Override
    public <T> void write(T payload) {
        if (payload == null) {
            LOG.debug("Payload was null. Skipping.");
            return;
        }

        String canonicalClassName = AutoValueUtils.getCanonicalName(payload.getClass());
        write(canonicalClassName, payload);
    }

    @Override
    public <T> void write(String key, T payload) {
        if (payload == null) {
            LOG.debug("Payload was null. Skipping.");
            return;
        }

        ClusterConfig clusterConfig = ClusterConfig.create(key, payload, nodeId.getNodeId());

        collection.replaceOne(Filters.eq("type", key), clusterConfig, new ReplaceOptions().upsert(true));

        ClusterConfigChangedEvent event = ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), nodeId.getNodeId(), key);
        clusterEventBus.post(event);
    }

    @Override
    public <T> int remove(Class<T> type) {
        final String canonicalName = type.getCanonicalName();
        return Ints.saturatedCast(collection.deleteMany(Filters.eq("type", canonicalName)).getDeletedCount());
    }

    @Override
    public Set<Class<?>> list() {
        final ImmutableSet.Builder<Class<?>> classes = ImmutableSet.builder();

        collection.find().forEach(clusterConfig -> {
            final String type = clusterConfig.type();
            try {
                final Class<?> cls = chainingClassLoader.loadClassSafely(type);
                classes.add(cls);
            } catch (ClassNotFoundException e) {
                LOG.debug("Couldn't find configuration class \"{}\"", type, e);
            } catch (UnsafeClassLoadingAttemptException e) {
                LOG.warn("Couldn't load class <{}>.", type, e);
            }
        });

        return classes.build();
    }
}
