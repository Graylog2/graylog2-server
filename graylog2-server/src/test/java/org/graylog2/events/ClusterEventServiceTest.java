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
import com.github.joschi.jadconfig.util.Size;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.DeadEvent;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.RestrictedChainingClassLoader;
import org.graylog2.security.SafeClasses;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.system.debug.DebugEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ClusterEventServiceTest {
    private static final DateTime TIME = new DateTime(2015, 4, 1, 0, 0, DateTimeZone.UTC);
    private static final Size COLLECTION_SIZE = Size.megabytes(100);
    private static final Offset initialOffset = new Offset(TIME.minusSeconds(1).toDate(), null);

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private final NodeId nodeId = new SimpleNodeId("ID");
    @Spy
    private EventBus serverEventBus;
    @Spy
    private ClusterEventBus clusterEventBus;
    private MongoConnection mongoConnection;
    private ClusterEventService clusterEventService;
    private MongoJackObjectMapperProvider objectMapperProvider;
    private MongoCollection<Document> collection;

    @BeforeEach
    public void setUpService(MongoCollections mongoCollections) throws Exception {
        DateTimeUtils.setCurrentMillisFixed(TIME.getMillis());

        this.mongoConnection = mongoCollections.connection();
        this.objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);
        this.collection = mongoCollections.mongoConnection().getMongoDatabase().getCollection(ClusterEventService.COLLECTION_NAME);

        this.clusterEventService = new ClusterEventService(
                objectMapperProvider,
                mongoConnection,
                nodeId,
                new RestrictedChainingClassLoader(new ChainingClassLoader(getClass().getClassLoader()),
                        new SafeClasses(Stream.of(SimpleEvent.class, DebugEvent.class, Safe.class).map(Class::getName).collect(Collectors.toSet()))),
                serverEventBus,
                clusterEventBus,
                initialOffset,
                COLLECTION_SIZE
        );
    }

    @AfterEach
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
        mongoConnection.getMongoDatabase().drop();
    }

    @Test
    public void clusterEventServiceRegistersItselfWithClusterEventBus() throws Exception {
        verify(clusterEventBus, times(1)).registerClusterEventSubscriber(clusterEventService);
    }

    @Test
    public void runHandlesInvalidPayloadsGracefully() throws Exception {
        saveEvent(TIME.toDate(), SimpleEvent.class.getCanonicalName(), ImmutableMap.of("HAHA", "test"));

        assertThat(collection.countDocuments()).isEqualTo(1L);

        runService();

        assertThat(collection.countDocuments()).isEqualTo(1L);

        verify(serverEventBus, never()).post(any());
        verify(clusterEventBus, never()).post(any());
    }

    private void runService() {
        clusterEventService.iterateEvents(clusterEventService.eventsIterable(initialOffset).iterator());
    }

    @Test
    public void serverEventBusDispatchesTypedEvents() throws Exception {
        final SimpleEventHandler handler = new SimpleEventHandler();
        serverEventBus.register(handler);

        assertThat(collection.countDocuments()).isEqualTo(0L);
        saveEvent(TIME.toDate(), SimpleEvent.class.getCanonicalName(), ImmutableMap.of("payload", "test"));
        assertThat(collection.countDocuments()).isEqualTo(1L);
        assertThat(handler.invocations).hasValue(0);

        runService();

        assertThat(handler.invocations).hasValue(1);
        assertThat(collection.countDocuments()).isEqualTo(1L);

        verify(serverEventBus, times(1)).post(any(SimpleEvent.class));
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    public void runHandlesAutoValueCorrectly() throws Exception {
        final DebugEvent event = DebugEvent.create("Node ID", TIME, "test");
        saveEvent(TIME.toDate(), DebugEvent.class.getCanonicalName(), objectMapper.convertValue(event, Map.class));

        assertThat(collection.countDocuments()).isEqualTo(1L);

        runService();

        assertThat(collection.countDocuments()).isEqualTo(1L);

        verify(serverEventBus, times(1)).post(event);
        verify(clusterEventBus, never()).post(event);
    }

    @Test
    public void testRun() throws Exception {
        saveEvent(TIME.toDate(), SimpleEvent.class.getCanonicalName(), ImmutableMap.of("payload", "test"));

        assertThat(collection.countDocuments()).isEqualTo(1L);

        runService();

        assertThat(collection.countDocuments()).isEqualTo(1L);

        verify(serverEventBus, times(1)).post(new SimpleEvent("test"));
        verify(clusterEventBus, never()).post(any());
    }

    @Test
    public void testPublishClusterEvent() throws Exception {
        SimpleEvent event = new SimpleEvent("test");

        assertThat(collection.countDocuments()).isEqualTo(0L);

        clusterEventService.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.countDocuments()).isEqualTo(1L);

        final var savedEvent = collection.find().first();

        assertThat(savedEvent.getString("producer")).isEqualTo(nodeId.getNodeId());
        assertThat(savedEvent.getString("event_class")).isEqualTo(SimpleEvent.class.getCanonicalName());

        final var payload = savedEvent.get("payload", Document.class);
        assertThat(payload.getString("payload")).isEqualTo("test");
    }

    @Test
    public void publishClusterEventHandlesAutoValueCorrectly() throws Exception {
        @SuppressWarnings("deprecation")
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        DebugEvent event = DebugEvent.create("Node ID", "Test");

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventService.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();

        assertThat((String) dbObject.get("producer")).isEqualTo(nodeId.getNodeId());
        assertThat((String) dbObject.get("event_class")).isEqualTo(DebugEvent.class.getCanonicalName());
    }

    @Test
    public void publishClusterEventSkipsDeadEvent() throws Exception {
        @SuppressWarnings("deprecation")
        DBCollection collection = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        DeadEvent event = new DeadEvent(clusterEventBus, new SimpleEvent("test"));

        assertThat(collection.count()).isEqualTo(0L);

        clusterEventService.publishClusterEvent(event);

        verify(clusterEventBus, never()).post(any());
        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    public void prepareCollectionCreatesIndexesOnExistingCollection() throws Exception {
        @SuppressWarnings("deprecation")
        DBCollection original = mongoConnection.getDatabase().getCollection(ClusterEventService.COLLECTION_NAME);
        original.dropIndexes();
        assertThat(original.getName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        assertThat(original.getIndexInfo()).hasSize(1);

        final var collection = ClusterEventService.prepareCollection(mongoConnection, objectMapperProvider, COLLECTION_SIZE);
        assertThat(collection.getNamespace().getCollectionName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        assertThat(collection.listIndexes()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    public void prepareCollectionCreatesCollectionIfItDoesNotExist() throws Exception {
        @SuppressWarnings("deprecation")
        final DB database = mongoConnection.getDatabase();
        database.getCollection(ClusterEventService.COLLECTION_NAME).drop();
        assertThat(database.collectionExists(ClusterEventService.COLLECTION_NAME)).isFalse();
        final var collection = ClusterEventService.prepareCollection(mongoConnection, objectMapperProvider, COLLECTION_SIZE);

        assertThat(collection.getNamespace().getCollectionName()).isEqualTo(ClusterEventService.COLLECTION_NAME);
        assertThat(collection.listIndexes()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    public void localEventIsPostedToServerBusImmediately() {
        SimpleEvent event = new SimpleEvent("test");

        clusterEventService.publishClusterEvent(event);

        verify(serverEventBus, times(1)).post(event);
    }

    @Test
    public void localEventIsNotProcessedFromDB() {
        saveEvent(TIME.toDate(), SimpleEvent.class.getCanonicalName(), ImmutableMap.of("payload", "test"), true);

        runService();

        verify(serverEventBus, never()).post(any());
        verify(clusterEventBus, never()).post(any());
    }

    private static volatile String constructorArgument;

    public static class Unsafe {
        public Unsafe(String param) {
            constructorArgument = param;
        }
    }

    public static class Safe {
        public Safe(String param) {
            constructorArgument = param;
        }
    }

    private void saveEvent(Date timestamp, String eventClass, Object payload) {
        saveEvent(timestamp, eventClass, payload, false);
    }

    private void saveEvent(Date timestamp, String eventClass, Object payload, boolean selfProduced) {
        final var event = new Document()
                .append("timestamp", timestamp)
                .append("producer", selfProduced ? nodeId.toString() : "TEST-PRODUCER")
                .append("event_class", eventClass)
                .append("payload", payload);
        collection.insertOne(event);
    }

    @Test
    public void testInstantiatesSafeEventClass() {
        saveEvent(TIME.toDate(), Safe.class.getName(), "this-is-safe");

        constructorArgument = null;
        runService();
        assertThat(constructorArgument).isEqualTo("this-is-safe");
    }

    @Test
    public void testIgnoresUnsafeEventClass() {
        saveEvent(TIME.toDate(), Unsafe.class.getName(), "this-is-unsafe");

        constructorArgument = null;
        runService();
        assertThat(constructorArgument).isNull();
    }

    @ExtendWith(MongoDBExtension.class)
    public static class SimpleEventHandler {
        final AtomicInteger invocations = new AtomicInteger();

        @Subscribe
        @SuppressWarnings("unused")
        public void handleSimpleEvent(SimpleEvent event) {
            invocations.incrementAndGet();
        }
    }
}
