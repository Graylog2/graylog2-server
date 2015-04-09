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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Uninterruptibles;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
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
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class ClusterEventService extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventService.class);

    @VisibleForTesting
    static final String COLLECTION_NAME = "cluster_events";

    private final JacksonDBCollection<ClusterEvent, String> dbCollection;
    private final NodeId nodeId;
    private final ObjectMapper objectMapper;
    private final EventBus serverEventBus;
    private final EventBus clusterEventBus;

    @Inject
    public ClusterEventService(final MongoJackObjectMapperProvider mapperProvider,
                               final MongoConnection mongoConnection,
                               final NodeId nodeId,
                               final ObjectMapper objectMapper,
                               final EventBus serverEventBus,
                               @Named("cluster_event_bus") final EventBus clusterEventBus) {
        this(JacksonDBCollection.wrap(prepareCollection(mongoConnection), ClusterEvent.class, String.class, mapperProvider.get()),
                nodeId, objectMapper, serverEventBus, clusterEventBus);
    }

    ClusterEventService(final JacksonDBCollection<ClusterEvent, String> dbCollection,
                        final NodeId nodeId,
                        final ObjectMapper objectMapper,
                        final EventBus serverEventBus,
                        final EventBus clusterEventBus) {
        this.nodeId = checkNotNull(nodeId);
        this.dbCollection = checkNotNull(dbCollection);
        this.objectMapper = checkNotNull(objectMapper);
        this.serverEventBus = checkNotNull(serverEventBus);
        this.clusterEventBus = checkNotNull(clusterEventBus);

        this.clusterEventBus.register(this);
    }

    @VisibleForTesting
    static DBCollection prepareCollection(final MongoConnection mongoConnection) {
        final DB db = mongoConnection.getDatabase();

        final DBCollection coll;
        if (!db.collectionExists(COLLECTION_NAME)) {
            final DBObject options = BasicDBObjectBuilder.start()
                    .add("capped", true)
                    .add("size", 32768)
                    .get();

            coll = db.createCollection(COLLECTION_NAME, options);
        } else {
            coll = db.getCollection(COLLECTION_NAME);
        }

        if (!db.getCollection(COLLECTION_NAME).isCapped()) {
            LOG.warn("The MongoDB collection \"{}\" should be capped but isn't. "
                    + "Please drop the collection and restart Graylog", COLLECTION_NAME);
        }

        coll.createIndex(DBSort.asc("producer"));
        coll.createIndex(DBSort.asc("consumers"));
        coll.addOption(Bytes.QUERYOPTION_TAILABLE | Bytes.QUERYOPTION_AWAITDATA);

        return coll;
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

        return dbCollection.find(query)
                .sort(DBSort.asc("$natural"))
                .addOption(Bytes.QUERYOPTION_TAILABLE)
                .addOption(Bytes.QUERYOPTION_AWAITDATA);
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

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
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

            // Don't overwhelm the server
            Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);
        }
    }

    @Subscribe
    public void publishClusterEvent(Object event) {
        final String className = event.getClass().getCanonicalName();
        final ClusterEvent clusterEvent = ClusterEvent.create(nodeId.toString(), className, event);
        final String id = dbCollection.save(clusterEvent).getSavedId();
        LOG.debug("Published cluster event with ID <{}> and type <{}>", id, className);
    }
}
