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
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterConfigServiceImpl implements ClusterConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterConfigServiceImpl.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_config";

    private final JacksonDBCollection<ClusterConfig, String> dbCollection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final ChainingClassLoader chainingClassLoader;
    private final EventBus clusterEventBus;

    @Inject
    public ClusterConfigServiceImpl(final MongoJackObjectMapperProvider mapperProvider,
                                    final MongoConnection mongoConnection,
                                    final NodeId nodeId,
                                    final ChainingClassLoader chainingClassLoader,
                                    final ClusterEventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(prepareCollection(mongoConnection), ClusterConfig.class, String.class, mapperProvider.get()),
                nodeId, mapperProvider.get(), chainingClassLoader, clusterEventBus);
    }

    private ClusterConfigServiceImpl(final JacksonDBCollection<ClusterConfig, String> dbCollection,
                                     final NodeId nodeId,
                                     final ObjectMapper objectMapper,
                                     final ChainingClassLoader chainingClassLoader,
                                     final EventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.dbCollection = checkNotNull(dbCollection);
        this.objectMapper = checkNotNull(objectMapper);
        this.chainingClassLoader = chainingClassLoader;
        this.clusterEventBus = checkNotNull(clusterEventBus);
    }

    @VisibleForTesting
    static DBCollection prepareCollection(final MongoConnection mongoConnection) {
        DBCollection coll = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        coll.createIndex(DBSort.asc("type"), "unique_type", true);
        coll.setWriteConcern(WriteConcern.JOURNALED);

        return coll;
    }

    private <T> T extractPayload(Object payload, Class<T> type) {
        try {
            return objectMapper.convertValue(payload, type);
        } catch (IllegalArgumentException e) {
            LOG.error("Error while deserializing payload", e);
            return null;
        }
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        ClusterConfig config = dbCollection.findOne(DBQuery.is("type", key));

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

    @Override
    public <T> T get(Class<T> type) {
        return get(type.getCanonicalName(), type);
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

        ClusterConfig clusterConfig = ClusterConfig.create(key, payload, nodeId.toString());

        dbCollection.update(DBQuery.is("type", key), clusterConfig, true, false, WriteConcern.JOURNALED);

        ClusterConfigChangedEvent event = ClusterConfigChangedEvent.create(
                DateTime.now(DateTimeZone.UTC), nodeId.toString(), key);
        clusterEventBus.post(event);
    }

    @Override
    public <T> int remove(Class<T> type) {
        final String canonicalName = type.getCanonicalName();
        final WriteResult<ClusterConfig, String> result = dbCollection.remove(DBQuery.is("type", canonicalName));
        return result.getN();
    }

    @Override
    public Set<Class<?>> list() {
        final ImmutableSet.Builder<Class<?>> classes = ImmutableSet.builder();

        try (DBCursor<ClusterConfig> clusterConfigs = dbCollection.find()) {
            for (ClusterConfig clusterConfig : clusterConfigs) {
                final String type = clusterConfig.type();
                try {
                    final Class<?> cls = chainingClassLoader.loadClass(type);
                    classes.add(cls);
                } catch (ClassNotFoundException e) {
                    LOG.debug("Couldn't find configuration class \"{}\"", type, e);
                }
            }
        }

        return classes.build();
    }
}
