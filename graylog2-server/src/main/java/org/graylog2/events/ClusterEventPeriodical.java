/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
import org.mongojack.DBCursor;
import org.mongojack.DBSort;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterEventPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventPeriodical.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_events";

    private final JacksonDBCollection<ClusterEvent, String> dbCollection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final EventBus serverEventBus;

    @Inject
    public ClusterEventPeriodical(final MongoJackObjectMapperProvider mapperProvider,
                                  final MongoConnection mongoConnection,
                                  final NodeId nodeId,
                                  final ObjectMapper objectMapper,
                                  final EventBus serverEventBus,
                                  @Named("cluster_event_bus") final EventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(prepareCollection(mongoConnection), ClusterEvent.class, String.class, mapperProvider.get()),
                nodeId, objectMapper, serverEventBus, clusterEventBus);
    }

    ClusterEventPeriodical(final JacksonDBCollection<ClusterEvent, String> dbCollection,
                           final NodeId nodeId,
                           final ObjectMapper objectMapper,
                           final EventBus serverEventBus,
                           final EventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.dbCollection = checkNotNull(dbCollection);
        this.objectMapper = checkNotNull(objectMapper);
        this.serverEventBus = checkNotNull(serverEventBus);

        checkNotNull(clusterEventBus).register(this);
    }

    @VisibleForTesting
    static DBCollection prepareCollection(final MongoConnection mongoConnection) {
        final DB db = mongoConnection.getDatabase();

        DBCollection coll = db.getCollection(COLLECTION_NAME);

        coll.createIndex(DBSort.desc("timestamp"));
        coll.createIndex(DBSort.asc("producer"));
        coll.createIndex(DBSort.asc("consumers"));

        coll.setWriteConcern(WriteConcern.MAJORITY);

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
        try {
            LOG.debug("Opening MongoDB cursor on \"{}\"", COLLECTION_NAME);
            final DBCursor<ClusterEvent> cursor = eventCursor(nodeId);
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

        final String className = getCanonicalName(event.getClass());
        final ClusterEvent clusterEvent = ClusterEvent.create(nodeId.toString(), className, event);

        try {
            final String id = dbCollection.save(clusterEvent).getSavedId();
            LOG.debug("Published cluster event with ID <{}> and type <{}>", id, className);
        } catch (MongoException e) {
            LOG.error("Couldn't publish cluster event of type <" + className + ">", e);
        }
    }

    private DBCursor<ClusterEvent> eventCursor(NodeId nodeId) {
        // Resorting to ugly MongoDB Java Client because of https://github.com/devbliss/mongojack/issues/88
        final DBObject producerClause = new BasicDBObject("producer", new BasicDBObject("$ne", nodeId.toString()));
        final BasicDBList consumersList = new BasicDBList();
        consumersList.add(nodeId.toString());
        final DBObject consumersClause = new BasicDBObject("consumers", new BasicDBObject("$nin", consumersList));
        final BasicDBList and = new BasicDBList();
        and.add(producerClause);
        and.add(consumersClause);
        final DBObject query = new BasicDBObject("$and", and);

        return dbCollection.find(query).sort(DBSort.desc("timestamp"));
    }

    private void updateConsumers(final String eventId, final NodeId nodeId) {
        final WriteResult<ClusterEvent, String> writeResult = dbCollection.updateById(eventId, DBUpdate.addToSet("consumers", nodeId.toString()));
    }

    private Object extractPayload(Object payload, String eventClass) {
        try {
            final Class<?> clazz = Class.forName(eventClass);
            return objectMapper.convertValue(payload, clazz);
        } catch (ClassNotFoundException e) {
            LOG.debug("Couldn't load class <" + eventClass + "> for event", e);
            return null;
        } catch (IllegalArgumentException e) {
            LOG.debug("Error while deserializing payload", e);
            return null;

        }
    }

    /**
     * Get the canonical class name of the provided {@link Class} with special handling of Google AutoValue classes.
     *
     * @param aClass a class
     * @return the canonical class name of {@code aClass} or its super class in case of an auto-generated class by
     * Google AutoValue
     * @see Class#getCanonicalName()
     * @see com.google.auto.value.AutoValue
     */
    private String getCanonicalName(final Class<?> aClass) {
        final Class<?> cls;
        if (aClass.getSimpleName().startsWith("AutoValue_")) {
            cls = aClass.getSuperclass();
        } else {
            cls = aClass;
        }

        return cls.getCanonicalName();
    }
}
