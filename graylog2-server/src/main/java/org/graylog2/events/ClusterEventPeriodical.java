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
package org.graylog2.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.UnsafeClassLoadingAttemptException;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

public class ClusterEventPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventPeriodical.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_events";

    private final MongoCollection<ClusterEvent> collection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final EventBus serverEventBus;
    private final RestrictedChainingClassLoader chainingClassLoader;

    @Inject
    public ClusterEventPeriodical(final MongoJackObjectMapperProvider mapperProvider,
                                  final MongoConnection mongoConnection,
                                  final NodeId nodeId,
                                  final RestrictedChainingClassLoader chainingClassLoader,
                                  final EventBus serverEventBus,
                                  final ClusterEventBus clusterEventBus) {
        this.nodeId = nodeId;
        this.objectMapper = mapperProvider.get();
        this.chainingClassLoader = chainingClassLoader;
        this.serverEventBus = serverEventBus;
        this.collection = prepareCollection(mongoConnection, mapperProvider);

        clusterEventBus.registerClusterEventSubscriber(this);
    }

    @VisibleForTesting
    static MongoCollection<ClusterEvent> prepareCollection(final MongoConnection mongoConnection,
                                                           final MongoJackObjectMapperProvider mapperProvider) {
        final var collection = new MongoCollections(mapperProvider, mongoConnection)
                .collection(COLLECTION_NAME, ClusterEvent.class)
                .withWriteConcern(WriteConcern.JOURNALED);

        collection.createIndex(Indexes.ascending(
                "timestamp",
                "producer",
                "consumers"));

        return collection;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean leaderOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        LOG.debug("Opening MongoDB cursor on \"{}\"", COLLECTION_NAME);
        try {
            final FindIterable<ClusterEvent> eventsIterable = eventsIterable(nodeId);
            if (LOG.isTraceEnabled()) {
                LOG.trace("MongoDB query plan: {}", eventsIterable.explain());
            }

            try (final var stream = MongoUtils.stream(eventsIterable)) {
                stream.forEach(clusterEvent -> {
                    LOG.trace("Processing cluster event: {}", clusterEvent);

                    Object payload = extractPayload(clusterEvent.payload(), clusterEvent.eventClass());
                    if (payload != null) {
                        serverEventBus.post(payload);
                    } else {
                        LOG.warn("Couldn't extract payload of cluster event with ID <{}>", clusterEvent.id());
                        LOG.debug("Invalid payload in cluster event: {}", clusterEvent);
                    }

                    updateConsumers(clusterEvent.id(), nodeId);
                });
            }
        } catch (Exception e) {
            LOG.warn("Error while reading cluster events from MongoDB, retrying.", e);
        }
    }

    @Subscribe
    public void publishClusterEvent(Object event) {
        if (event instanceof DeadEvent) {
            LOG.debug("Skipping DeadEvent on cluster event bus");
            return;
        }

        final String className = AutoValueUtils.getCanonicalName(event.getClass());
        final ClusterEvent clusterEvent = ClusterEvent.create(nodeId.getNodeId(), className, Collections.singleton(nodeId.getNodeId()), event);

        try {
            final String id = MongoUtils.insertedIdAsString(collection.insertOne(clusterEvent));
            // We are handling a locally generated event, so we can speed up processing by posting it to the local event
            // bus immediately. Due to having added the local node id to its list of consumers, it will not be picked up
            // by the db cursor again, avoiding double processing of the event. See #11263 for details.
            serverEventBus.post(event);
            LOG.debug("Published cluster event with ID <{}> and type <{}>", id, className);
        } catch (MongoException e) {
            LOG.error("Couldn't publish cluster event of type <" + className + ">", e);
        }
    }

    private FindIterable<ClusterEvent> eventsIterable(NodeId nodeId) {
        return collection.find(Filters.nin("consumers", nodeId.getNodeId()))
                .sort(Sorts.ascending("timestamp"));
    }

    private void updateConsumers(final String eventId, final NodeId nodeId) {
        collection.updateOne(MongoUtils.idEq(eventId), Updates.addToSet("consumers", nodeId.getNodeId()));
    }

    private Object extractPayload(Object payload, String eventClass) {
        try {
            final Class<?> clazz = chainingClassLoader.loadClassSafely(eventClass);
            return objectMapper.convertValue(payload, clazz);
        } catch (ClassNotFoundException e) {
            LOG.debug("Couldn't load class <" + eventClass + "> for event", e);
        } catch (IllegalArgumentException e) {
            LOG.debug("Error while deserializing payload", e);
        } catch (UnsafeClassLoadingAttemptException e) {
            LOG.warn("Couldn't load class <{}>.", eventClass, e);
        }
        return null;
    }
}
