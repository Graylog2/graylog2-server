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
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.system.debug.DebugEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterEventPeriodicalTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    private static final DateTime TIME = new DateTime(2015, 4, 1, 0, 0, DateTimeZone.UTC);

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private NodeId nodeId;
    @Spy
    private EventBus serverEventBus;
    @Spy
    private ClusterEventBus clusterEventBus;
    private MongoConnection mongoConnection;
    private ClusterEventPeriodical clusterEventPeriodical;

    @Before
    public void setUpService() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(TIME.getMillis());

        this.mongoConnection = mongoRule.getMongoConnection();

        MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeId.toString()).thenReturn("ID");

        this.clusterEventPeriodical = new ClusterEventPeriodical(
                provider,
                mongoRule.getMongoConnection(),
                nodeId,
                objectMapper,
                new ChainingClassLoader(getClass().getClassLoader()),
                serverEventBus,
                clusterEventBus
        );
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    public void clusterEventServiceRegistersItselfWithClusterEventBus() throws Exception {
        verify(clusterEventBus, times(1)).registerClusterEventSubscriber(clusterEventPeriodical);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void runHandlesInvalidPayloadsGracefully() throws Exception {
        DBObject event = new BasicDBObjectBuilder()
                .add("timestamp", TIME.getMillis())
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("HAHA", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        collection.save(event);

        assertThat(collection.count()).isEqualTo(1L);

        clusterEventPeriodical.run();

        assertThat(collection.count()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) collection.findOne().get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        verify(serverEventBus, never()).post(any());
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void serverEventBusDispatchesTypedEvents() throws Exception {
        final SimpleEventHandler handler = new SimpleEventHandler();
        serverEventBus.register(handler);

        DBObject event = new BasicDBObjectBuilder()
                .add("timestamp", TIME.getMillis())
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("payload", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(collection.save(event).getN()).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(1L);
        assertThat(handler.invocations).isEqualTo(0);

        clusterEventPeriodical.run();

        assertThat(handler.invocations).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) collection.findOne().get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        verify(serverEventBus, times(1)).post(any(SimpleEvent.class));
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void runHandlesAutoValueCorrectly() throws Exception {
        final DebugEvent event = DebugEvent.create("Node ID", TIME, "test");
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("timestamp", TIME.getMillis())
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", DebugEvent.class.getCanonicalName())
                .add("payload", objectMapper.convertValue(event, Map.class))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        clusterEventPeriodical.run();

        assertThat(collection.count()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) collection.findOne().get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        verify(serverEventBus, times(1)).post(event);
        verify(clusterEventBus, never()).post(event);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testRun() throws Exception {
        DBObject event = new BasicDBObjectBuilder()
                .add("timestamp", TIME.getMillis())
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("payload", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        collection.save(event);

        assertThat(collection.count()).isEqualTo(1L);

        clusterEventPeriodical.run();

        assertThat(collection.count()).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) collection.findOne().get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        verify(serverEventBus, times(1)).post(new SimpleEvent("test"));
        verify(clusterEventBus, never()).post(event);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testPublishClusterEvent() throws Exception {
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        SimpleEvent event = new SimpleEvent("test");

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventPeriodical.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        assertThat((String) dbObject.get("producer")).isEqualTo(nodeId.toString());
        assertThat((String) dbObject.get("event_class")).isEqualTo(SimpleEvent.class.getCanonicalName());

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) dbObject.get("payload");
        assertThat(payload).containsEntry("payload", "test");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void publishClusterEventHandlesAutoValueCorrectly() throws Exception {
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        DebugEvent event = DebugEvent.create("Node ID", "Test");

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventPeriodical.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        assertThat((String) dbObject.get("producer")).isEqualTo(nodeId.toString());
        assertThat((String) dbObject.get("event_class")).isEqualTo(DebugEvent.class.getCanonicalName());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void publishClusterEventSkipsDeadEvent() throws Exception {
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        DeadEvent event = new DeadEvent(clusterEventBus, new SimpleEvent("test"));

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventPeriodical.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    public void prepareCollectionCreatesIndexesOnExistingCollection() throws Exception {
        DBCollection original = mongoConnection.getDatabase().getCollection(ClusterEventPeriodical.COLLECTION_NAME);
        original.dropIndexes();
        assertThat(original.getName()).isEqualTo(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(original.getIndexInfo()).hasSize(1);

        DBCollection collection = ClusterEventPeriodical.prepareCollection(mongoConnection);
        assertThat(collection.getName()).isEqualTo(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void prepareCollectionCreatesCollectionIfItDoesNotExist() throws Exception {
        assertThat(mongoConnection.getDatabase().collectionExists(ClusterEventPeriodical.COLLECTION_NAME)).isFalse();
        DBCollection collection = ClusterEventPeriodical.prepareCollection(mongoConnection);

        assertThat(collection.getName()).isEqualTo(ClusterEventPeriodical.COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    public static class SimpleEventHandler {
        public volatile int invocations = 0;

        @Subscribe
        public void handleSimpleEvent(SimpleEvent event) {
            invocations++;
        }
    }
}