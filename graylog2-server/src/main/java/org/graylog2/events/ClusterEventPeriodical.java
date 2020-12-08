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
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.utilities.AutoValueUtils;
import org.mongojack.DBCursor;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterEventPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventPeriodical.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_events";

    private final JacksonDBCollection<ClusterEvent, String> dbCollection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final EventBus serverEventBus;
    private final ChainingClassLoader chainingClassLoader;

    @Inject
    public ClusterEventPeriodical(final MongoJackObjectMapperProvider mapperProvider,
                                  final MongoConnection mongoConnection,
                                  final NodeId nodeId,
                                  final ChainingClassLoader chainingClassLoader,
                                  final EventBus serverEventBus,
                                  final ClusterEventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(prepareCollection(mongoConnection), ClusterEvent.class, String.class, mapperProvider.get()),
                nodeId, mapperProvider.get(), chainingClassLoader, serverEventBus, clusterEventBus);
    }

    private ClusterEventPeriodical(final JacksonDBCollection<ClusterEvent, String> dbCollection,
                           final NodeId nodeId,
                           final ObjectMapper objectMapper,
                           final ChainingClassLoader chainingClassLoader,
                           final EventBus serverEventBus,
                           final ClusterEventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.dbCollection = checkNotNull(dbCollection);
        this.objectMapper = checkNotNull(objectMapper);
        this.chainingClassLoader = chainingClassLoader;
        this.serverEventBus = checkNotNull(serverEventBus);

        checkNotNull(clusterEventBus).registerClusterEventSubscriber(this);
    }

    @VisibleForTesting
    static DBCollection prepareCollection(final MongoConnection mongoConnection) {
        final DB db = mongoConnection.getDatabase();

        DBCollection coll = db.getCollection(COLLECTION_NAME);

        coll.createIndex(DBSort
                .asc("timestamp")
                .asc("producer")
                .asc("consumers"));

        coll.setWriteConcern(WriteConcern.JOURNALED);

        return coll;
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
    public boolean masterOnly() {
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
        try (DBCursor<ClusterEvent> cursor = eventCursor(nodeId)) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("MongoDB query plan: {}", cursor.explain());
            }

            while (cursor.hasNext()) {
                ClusterEvent clusterEvent = cursor.next();
                LOG.trace("Processing cluster event: {}", clusterEvent);

                Object payload = extractPayload(clusterEvent.payload(), clusterEvent.eventClass());
                if (payload != null) {
                    serverEventBus.post(payload);
                } else {
                    LOG.warn("Couldn't extract payload of cluster event with ID <{}>", clusterEvent.id());
                    LOG.debug("Invalid payload in cluster event: {}", clusterEvent);
                }

                updateConsumers(clusterEvent.id(), nodeId);
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
        final ClusterEvent clusterEvent = ClusterEvent.create(nodeId.toString(), className, event);

        try {
            final String id = dbCollection.save(clusterEvent, WriteConcern.JOURNALED).getSavedId();
            LOG.debug("Published cluster event with ID <{}> and type <{}>", id, className);
        } catch (MongoException e) {
            LOG.error("Couldn't publish cluster event of type <" + className + ">", e);
        }
    }

    private DBCursor<ClusterEvent> eventCursor(NodeId nodeId) {
        // Resorting to ugly MongoDB Java Client because of https://github.com/devbliss/mongojack/issues/88
        final BasicDBList consumersList = new BasicDBList();
        consumersList.add(nodeId.toString());
        final DBObject query = new BasicDBObject("consumers", new BasicDBObject("$nin", consumersList));

        return dbCollection.find(query).sort(DBSort.asc("timestamp"));
    }

    private void updateConsumers(final String eventId, final NodeId nodeId) {
        dbCollection.updateById(eventId, DBUpdate.addToSet("consumers", nodeId.toString()));
    }

    private Object extractPayload(Object payload, String eventClass) {
        try {
            final Class<?> clazz = chainingClassLoader.loadClass(eventClass);
            return objectMapper.convertValue(payload, clazz);
        } catch (ClassNotFoundException e) {
            LOG.debug("Couldn't load class <" + eventClass + "> for event", e);
            return null;
        } catch (IllegalArgumentException e) {
            LOG.debug("Error while deserializing payload", e);
            return null;

        }
    }
}
