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
package org.graylog2.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ClusterConfigServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();
    private static final DateTime TIME = new DateTime(2015, 4, 1, 0, 0, DateTimeZone.UTC);
    private static final String COLLECTION_NAME = ClusterConfigServiceImpl.COLLECTION_NAME;

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");
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

        this.mongoConnection = mongoRule.getMongoConnection();

        MongoJackObjectMapperProvider provider = new MongoJackObjectMapperProvider(objectMapper);
        when(nodeId.toString()).thenReturn("ID");

        this.clusterConfigService = new ClusterConfigServiceImpl(
                provider,
                mongoRule.getMongoConnection(),
                nodeId,
                objectMapper,
                new ChainingClassLoader(getClass().getClassLoader()),
                clusterEventBus
        );
    }

    @After
    public void tearDown() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getReturnsExistingConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig customConfig = clusterConfigService.get(CustomConfig.class);
        assertThat(customConfig.text).isEqualTo("TEST");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getReturnsNullOnNonExistingConfig() throws Exception {
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        assertThat(clusterConfigService.get(CustomConfig.class)).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getReturnsNullOnInvalidPayload() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", "wrong payload")
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        assertThat(clusterConfigService.get(CustomConfig.class)).isNull();
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getOrDefaultReturnsExistingConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        CustomConfig customConfig = clusterConfigService.getOrDefault(CustomConfig.class, defaultValue);
        assertThat(customConfig.text).isEqualTo("TEST");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getOrDefaultReturnsDefaultValueOnNonExistingConfig() throws Exception {
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        assertThat(clusterConfigService.getOrDefault(CustomConfig.class, defaultValue)).isSameAs(defaultValue);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void getOrDefaultReturnsDefaultValueOnInvalidPayload() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", "wrong payload")
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);

        CustomConfig defaultValue = new CustomConfig();
        defaultValue.text = "DEFAULT";

        assertThat(clusterConfigService.getOrDefault(CustomConfig.class, defaultValue)).isSameAs(defaultValue);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void writeIgnoresNull() throws Exception {
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write(null);

        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void writePersistsClusterConfig() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        assertThat(collection.count()).isEqualTo(0L);

        clusterConfigService.write(customConfig);

        assertThat(collection.count()).isEqualTo(1L);

        DBObject dbObject = collection.findOne();
        assertThat((String) dbObject.get("type")).isEqualTo(CustomConfig.class.getCanonicalName());
        assertThat((String) dbObject.get("last_updated_by")).isEqualTo("ID");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void writeUpdatesExistingClusterConfig() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        DBObject seedObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "ORIGINAL"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "NOT ID")
                .get();
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void writePostsClusterConfigChangedEvent() throws Exception {
        CustomConfig customConfig = new CustomConfig();
        customConfig.text = "TEST";

        final ClusterConfigChangedEventHandler eventHandler = new ClusterConfigChangedEventHandler();
        clusterEventBus.registerClusterEventSubscriber(eventHandler);

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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void prepareCollectionCreatesCollectionIfItDoesNotExist() throws Exception {
        assertThat(mongoConnection.getDatabase().collectionExists(COLLECTION_NAME)).isFalse();
        DBCollection collection = ClusterConfigServiceImpl.prepareCollection(mongoConnection);

        assertThat(collection.getName()).isEqualTo(COLLECTION_NAME);
        assertThat(collection.getIndexInfo()).hasSize(2);
        assertThat(collection.getWriteConcern()).isEqualTo(WriteConcern.JOURNALED);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void removeDoesNothingIfConfigDoesNotExist() throws Exception {
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);

        assertThat(collection.count()).isEqualTo(0L);
        assertThat(clusterConfigService.remove(CustomConfig.class)).isEqualTo(0);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void removeSuccessfullyRemovesConfig() throws Exception {
        DBObject dbObject = new BasicDBObjectBuilder()
                .add("type", CustomConfig.class.getCanonicalName())
                .add("payload", Collections.singletonMap("text", "TEST"))
                .add("last_updated", TIME.toString())
                .add("last_updated_by", "ID")
                .get();
        final DBCollection collection = mongoConnection.getDatabase().getCollection(COLLECTION_NAME);
        collection.save(dbObject);

        assertThat(collection.count()).isEqualTo(1L);
        assertThat(clusterConfigService.remove(CustomConfig.class)).isEqualTo(1);
        assertThat(collection.count()).isEqualTo(0L);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void listReturnsAllClasses() throws Exception {
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
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void listIgnoresInvalidClasses() throws Exception {
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