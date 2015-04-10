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

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.Uninterruptibles;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.Bytes;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.ObjectIdSerializer;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.jackson.SizeSerializer;
import org.graylog2.shared.rest.RangeJsonSerializer;
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
import java.util.concurrent.TimeUnit;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterEventServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private ClusterEventService clusterEventService;
    private MongoConnection mongoConnection;
    @Mock
    private NodeId nodeId;
    @Spy
    private EventBus serverEventBus;
    @Spy
    private EventBus clusterEventBus;

    @Before
    public void setUpService() throws Exception {
        this.mongoConnection = mongoRule.getMongoConnection();

        ObjectMapper objectMapper = new ObjectMapper()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(new PropertyNamingStrategy.LowerCaseWithUnderscoresStrategy())
                .registerModule(new JodaModule())
                .registerModule(new GuavaModule())
                .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false))
                .registerModule(new SimpleModule()
                        .addSerializer(new ObjectIdSerializer())
                        .addSerializer(new RangeJsonSerializer())
                        .addSerializer(new SizeSerializer()));

        MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeId.toString()).thenReturn("ID");

        this.clusterEventService = new ClusterEventService(
                provider,
                mongoRule.getMongoConnection(),
                nodeId,
                objectMapper,
                serverEventBus,
                clusterEventBus
        );
    }

    @Test
    public void clusterEventServiceRegistersItselfWithClusterEventBus() throws Exception {
        verify(clusterEventBus, times(1)).register(clusterEventService);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void runHandlesInvalidPayloadsGracefully() throws Exception {
        DBObject event = new BasicDBObjectBuilder()
                .add("date", "2015-04-01T00:00:00.000Z")
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("HAHA", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        collection.save(event);

        assertThat(collection.count()).isEqualTo(1L);

        ServiceManager serviceManager = new ServiceManager(Collections.singleton(clusterEventService));

        serviceManager
                .startAsync()
                .awaitHealthy(1L, TimeUnit.SECONDS);

        assertThat(serviceManager.servicesByState().get(Service.State.RUNNING)).contains(clusterEventService);
        assertThat(clusterEventService.isRunning()).isTrue();

        Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) dbObject.get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        serviceManager
                .stopAsync()
                .awaitStopped(5L, TimeUnit.SECONDS);

        verify(serverEventBus, never()).post(any());
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void serverEventBusDispatchesTypedEvents() throws Exception {
        SimpleEventHandler handler = new SimpleEventHandler();
        serverEventBus.register(handler);

        DBObject event = new BasicDBObjectBuilder()
                .add("date", "2015-04-01T00:00:00.000Z")
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("payload", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        assertThat(collection.save(event).getN()).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(1L);
        assertThat(handler.invocations).isEqualTo(0);

        ServiceManager serviceManager = new ServiceManager(Collections.singleton(clusterEventService));

        serviceManager
                .startAsync()
                .awaitHealthy(1L, TimeUnit.SECONDS);

        assertThat(serviceManager.servicesByState().get(Service.State.RUNNING)).contains(clusterEventService);
        assertThat(clusterEventService.isRunning()).isTrue();

        Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);

        assertThat(handler.invocations).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) dbObject.get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        serviceManager
                .stopAsync()
                .awaitStopped(5L, TimeUnit.SECONDS);

        verify(serverEventBus, times(1)).post(any(SimpleEvent.class));
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testRun() throws Exception {
        DBObject event = new BasicDBObjectBuilder()
                .add("date", "2015-04-01T00:00:00.000Z")
                .add("producer", "TEST-PRODUCER")
                .add("consumers", Collections.emptyList())
                .add("event_class", SimpleEvent.class.getCanonicalName())
                .add("payload", ImmutableMap.of("payload", "test"))
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        collection.save(event);

        assertThat(collection.count()).isEqualTo(1L);

        ServiceManager serviceManager = new ServiceManager(Collections.singleton(clusterEventService));

        serviceManager
                .startAsync()
                .awaitHealthy(1L, TimeUnit.SECONDS);

        assertThat(serviceManager.servicesByState().get(Service.State.RUNNING)).contains(clusterEventService);
        assertThat(clusterEventService.isRunning()).isTrue();

        Uninterruptibles.sleepUninterruptibly(1L, TimeUnit.SECONDS);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        @SuppressWarnings("unchecked")
        final List<String> consumers = (List<String>) dbObject.get("consumers");
        assertThat(consumers).containsExactly(nodeId.toString());

        serviceManager
                .stopAsync()
                .awaitStopped(5L, TimeUnit.SECONDS);

        verify(serverEventBus, times(1)).post(new SimpleEvent("test"));
        verify(clusterEventBus, never()).post(event);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void testPublishClusterEvent() throws Exception {
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        SimpleEvent event = new SimpleEvent("test");

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventService.publishClusterEvent(event);

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
    public void publishClusterEventSkipsDeadEvent() throws Exception {
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        DeadEvent event = new DeadEvent(clusterEventBus, new SimpleEvent("test"));

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventService.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    public void prepareCollectionCreatesIndexesOnExistingCollection() throws Exception {
        DBCollection original = mongoConnection.getDatabase().createCollection(ClusterEventService.COLLECTION_NAME, null);
        original.dropIndexes();
        assertThat(original.getName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        assertThat(original.getIndexInfo()).hasSize(1);

        DBCollection collection = ClusterEventService.prepareCollection(mongoConnection);
        assertThat(collection.getName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(3);
        assertThat(collection.getOptions() & Bytes.QUERYOPTION_AWAITDATA).isEqualTo(Bytes.QUERYOPTION_AWAITDATA);
        assertThat(collection.getOptions() & Bytes.QUERYOPTION_TAILABLE).isEqualTo(Bytes.QUERYOPTION_TAILABLE);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.MAJORITY);
    }

    @Test
    public void prepareCollectionCreatesCollectionIfItDoesNotExist() throws Exception {
        DBCollection collection = ClusterEventService.prepareCollection(mongoConnection);

        assertThat(collection.getName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        // Not supported by Fongo at the moment.
        // assertThat(collection.isCapped()).isTrue();
        assertThat(collection.getIndexInfo()).hasSize(3);
        assertThat(collection.getOptions() & Bytes.QUERYOPTION_AWAITDATA).isEqualTo(Bytes.QUERYOPTION_AWAITDATA);
        assertThat(collection.getOptions() & Bytes.QUERYOPTION_TAILABLE).isEqualTo(Bytes.QUERYOPTION_TAILABLE);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.MAJORITY);
    }

    public static class SimpleEventHandler {
        public volatile int invocations = 0;
        @Subscribe
        public void handleSimpleEvent(SimpleEvent event) {
            invocations++;
        }
    }
}