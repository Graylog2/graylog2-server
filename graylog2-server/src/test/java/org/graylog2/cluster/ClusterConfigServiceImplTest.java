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
import com.google.common.eventbus.Subscribe;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ClusterConfigServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private static final DateTime TIME = new DateTime(2015, 4, 1, 0, 0, DateTimeZone.UTC);
    private static final String COLLECTION_NAME = ClusterConfigServiceImpl.COLLECTION_NAME;

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private NodeId nodeId;
    @Spy
    private ClusterEventBus clusterEventBus;
    private MongoConnection mongoConnection;
    private ClusterConfigService clusterConfigService;

    @Before
    public void setUpService() throws Exception {
        DateTimeUtils.setCurrentMillisFixed(TIME.getMillis());

        this.mongoConnection = mongodb.mongoConnection();

        MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeId.toString()).thenReturn("ID");

        this.clusterConfigService = new ClusterConfigServiceImpl(
                provider,
                mongodb.mongoConnection(),
                nodeId,
                new ChainingClassLoader(getClass().getClassLoader()),
                clusterEventBus
        );
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
        mongoConnection.getMongoDatabase().drop();
    }

    @Test
    public void getReturnsExistingConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig customConfig = clusterConfigService.get(CustomConfig.class);
        assertThat(customConfig.text).isEqualTo("TEST");
    }

    @Test
    public void getReturnsNullOnNonExistingConfig() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        assertThat(clusterConfigService.get(CustomConfig.class)).isNull();
    }

    @Test
    public void getReturnsNullOnInvalidPayload() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", "wrong payload")
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        assertThat(clusterConfigService.get(CustomConfig.class)).isNull();
    }

    @Test
    public void getWithKeyReturnsExistingConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", "foo")
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig customConfig = clusterConfigService.get("foo", CustomConfig.class);
        assertThat(customConfig).isInstanceOf(CustomConfig.class);
        assertThat(customConfig.text).isEqualTo("TEST");
    }

    @Test
    public void getWithKeyReturnsNullOnNonExistingConfig() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        assertThat(clusterConfigService.get("foo", CustomConfig.class)).isNull();
    }

    @Test
    public void getOrDefaultReturnsExistingConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        CustomConfig customConfig = clusterConfigService.getOrDefault(CustomConfig.class, defaultValue);
        assertThat(customConfig.text).isEqualTo("TEST");
    }

    @Test
    public void getOrDefaultReturnsDefaultValueOnNonExistingConfig() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        assertThat(clusterConfigService.getOrDefault(CustomConfig.class, defaultValue)).isSameAs(defaultValue);
    }

    @Test
    public void getOrDefaultReturnsDefaultValueOnInvalidPayload() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", "wrong payload")
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        assertThat(clusterConfigService.getOrDefault(CustomConfig.class, defaultValue)).isSameAs(defaultValue);
    }

    @Test
    public void writeIgnoresNull() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write(null);

        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    public void writePersistsClusterConfig() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write(customConfig);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();
        assertThat((String) dbObject.get("type")).isEqualTo(CustomConfig.class.getCanonicalName());
        assertThat((String) dbObject.get("last_updated_by")).isEqualTo("ID");
    }

    @Test
    public void writeWithCustomKeyPersistsClusterConfig() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write("foobar", customConfig);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();
        assertThat((String) dbObject.get("type")).isEqualTo("foobar");
        assertThat((String) dbObject.get("last_updated_by")).isEqualTo("ID");
    }

    @Test
    public void writeUpdatesExistingClusterConfig() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        DBObject seedObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "ORIGINAL"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "NOT ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(seedObject);
        assertThat(collection.count()).isEqualTo(1L);

        clusterConfigService.write(customConfig);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();
        assertThat((String) dbObject.get("type")).isEqualTo(CustomConfig.class.getCanonicalName());
        assertThat((String) dbObject.get("last_updated_by")).isEqualTo("ID");

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) dbObject.get("payload");

        assertThat(payload).containsEntry("text", "TEST");
    }

    @Test
    public void writePostsClusterConfigChangedEvent() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        final ClusterConfigChangedEventHandler eventHandler = new ClusterConfigChangedEventHandler();
        clusterEventBus.registerClusterEventSubscriber(eventHandler);

        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write(customConfig);

        assertThat(collection.count()).isEqualTo(1L);
        assertThat(eventHandler.event).isNotNull();
        assertThat(eventHandler.event.nodeId()).isEqualTo("ID");
        assertThat(eventHandler.event.type()).isEqualTo(CustomConfig.class.getCanonicalName());

        clusterEventBus.unregister(eventHandler);
    }


    @Test
    public void prepareCollectionCreatesIndexesOnExistingCollection() throws Exception {
        @SuppressWarnings("deprecation")
        DBCollection original = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        original.dropIndexes();
        assertThat(original.getName()).isEqualTo(COLLECTION_NAME);
        assertThat(original.getIndexInfo()).hasSize(1);

        DBCollection collection = ClusterConfigServiceImpl.prepareCollection(mongoConnection);
        assertThat(collection.getName()).isEqualTo(COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    public void prepareCollectionCreatesCollectionIfItDoesNotExist() throws Exception {
        @SuppressWarnings("deprecation")
        final DB database = mongoConnection.getDatabase();
        database.getCollection(COLLECTION_NAME).drop();
        assertThat(database.collectionExists(COLLECTION_NAME)).isFalse();
        DBCollection collection = ClusterConfigServiceImpl.prepareCollection(mongoConnection);

        assertThat(collection.getName()).isEqualTo(COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    public void removeDoesNothingIfConfigDoesNotExist() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);

        assertThat(collection.count()).isEqualTo(0L);
        assertThat(clusterConfigService.remove(CustomConfig.class)).isEqualTo(0);
    }

    @Test
    public void removeSuccessfullyRemovesConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);
        assertThat(clusterConfigService.remove(CustomConfig.class)).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    public void listReturnsAllClasses() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get());
        collection.save(new BasicDBObjectBuilder()
                .add("type", AnotherCustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get());

        assertThat(collection.count()).isEqualTo(2L);
        assertThat(clusterConfigService.list())
                .hasSize(2)
                .containsOnly(CustomConfig.class, AnotherCustomConfig.class);
    }

    @Test
    public void listIgnoresInvalidClasses() throws Exception {
        @SuppressWarnings("deprecation")
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get());
        collection.save(new BasicDBObjectBuilder()
                .add("type", "invalid.ClassName")
                .add("payload", Collections.emptyMap())
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get());

        assertThat(collection.count()).isEqualTo(2L);
        assertThat(clusterConfigService.list())
                .hasSize(1)
                .containsOnly(CustomConfig.class);
    }

    public static class ClusterConfigChangedEventHandler {
        public volatile ClusterConfigChangedEvent event;

        @SuppressWarnings("unused")
        @Subscribe
        public void handleSimpleEvent(ClusterConfigChangedEvent event) {
            this.event = ClusterConfigChangedEvent.create(
                    event.date(),
                    event.nodeId(),
                    event.type()
            );
        }
    }
}
