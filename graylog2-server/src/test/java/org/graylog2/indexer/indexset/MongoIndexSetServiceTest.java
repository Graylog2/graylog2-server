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
package org.graylog2.indexer.indexset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.Subscribe;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.buffers.processors.fakestreams.FakeStream;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategy;
import org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongojack.DBQuery;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class MongoIndexSetServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterEventBus clusterEventBus;
    private MongoIndexSetService indexSetService;
    private ClusterConfigService clusterConfigService;

    @Mock
    private StreamService streamService;
    @Mock
    private NodeId nodeId;

    @Before
    public void setUp() throws Exception {
        clusterEventBus = new ClusterEventBus();
        clusterConfigService = new ClusterConfigServiceImpl(objectMapperProvider, mongodb.mongoConnection(),
                nodeId, new ChainingClassLoader(getClass().getClassLoader()), clusterEventBus);
        indexSetService = new MongoIndexSetService(mongodb.mongoConnection(), objectMapperProvider, streamService, clusterConfigService, clusterEventBus);
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void getWithStringId() throws Exception {
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get("57f3d721a43c2d59cb750001");
        assertThat(indexSetConfig)
                .isPresent()
                .contains(
                        IndexSetConfig.create(
                                "57f3d721a43c2d59cb750001",
                                "Test 1",
                                "This is the index set configuration for Test 1",
                                true,
                                "test_1",
                                4,
                                1,
                                MessageCountRotationStrategy.class.getCanonicalName(),
                                MessageCountRotationStrategyConfig.create(1000),
                                NoopRetentionStrategy.class.getCanonicalName(),
                                NoopRetentionStrategyConfig.create(10),
                                ZonedDateTime.of(2016, 10, 4, 17, 0, 0, 0, ZoneOffset.UTC),
                                "standard",
                                "test_1",
                                null,
                                1,
                                false
                        )
                );
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void getReturnsExistingIndexSetConfig() throws Exception {
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(new ObjectId("57f3d721a43c2d59cb750001"));
        assertThat(indexSetConfig)
                .isPresent()
                .contains(
                        IndexSetConfig.create(
                                "57f3d721a43c2d59cb750001",
                                "Test 1",
                                "This is the index set configuration for Test 1",
                                true,
                                "test_1",
                                4,
                                1,
                                MessageCountRotationStrategy.class.getCanonicalName(),
                                MessageCountRotationStrategyConfig.create(1000),
                                NoopRetentionStrategy.class.getCanonicalName(),
                                NoopRetentionStrategyConfig.create(10),
                                ZonedDateTime.of(2016, 10, 4, 17, 0, 0, 0, ZoneOffset.UTC),
                                "standard",
                                "test_1",
                                null,
                                1,
                                false
                        )
                );
    }

    @Test
    public void getReturnsAbsentOptionalIfIndexSetConfigDoesNotExist() throws Exception {
        final Optional<IndexSetConfig> indexSetConfig = indexSetService.get(new ObjectId("57f3d3f0a43c2d595eb0a348"));
        assertThat(indexSetConfig).isEmpty();
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void getDefault() throws Exception {
        clusterConfigService.write(DefaultIndexSetConfig.create("57f3d721a43c2d59cb750002"));

        final IndexSetConfig indexSetConfig = indexSetService.getDefault();

        assertThat(indexSetConfig).isNotNull();
        assertThat(indexSetConfig.id()).isEqualTo("57f3d721a43c2d59cb750002");
    }

    @Test(expected = IllegalStateException.class)
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void getDefaultWithoutDefault() throws Exception {
        indexSetService.getDefault();
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void findOne() throws Exception {
        final Optional<IndexSetConfig> config3 = indexSetService.findOne(DBQuery.is("title", "Test 2"));
        assertThat(config3).isPresent();
        assertThat(config3.get().id()).isEqualTo("57f3d721a43c2d59cb750002");

        final Optional<IndexSetConfig> config4 = indexSetService.findOne(DBQuery.is("title", "__yolo"));
        assertThat(config4).isNotPresent();
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void findAll() throws Exception {
        final List<IndexSetConfig> configs = indexSetService.findAll();

        assertThat(configs)
                .isNotEmpty()
                .hasSize(3)
                .containsExactly(
                        IndexSetConfig.create(
                                "57f3d721a43c2d59cb750001",
                                "Test 1",
                                "This is the index set configuration for Test 1",
                                true,
                                "test_1",
                                4,
                                1,
                                MessageCountRotationStrategy.class.getCanonicalName(),
                                MessageCountRotationStrategyConfig.create(1000),
                                NoopRetentionStrategy.class.getCanonicalName(),
                                NoopRetentionStrategyConfig.create(10),
                                ZonedDateTime.of(2016, 10, 4, 17, 0, 0, 0, ZoneOffset.UTC),
                                "standard",
                                "test_1",
                                null,
                                1,
                                false
                        ),
                        IndexSetConfig.create(
                                "57f3d721a43c2d59cb750002",
                                "Test 2",
                                null,
                                true,
                                "test_2",
                                1,
                                0,
                                MessageCountRotationStrategy.class.getCanonicalName(),
                                MessageCountRotationStrategyConfig.create(2500),
                                NoopRetentionStrategy.class.getCanonicalName(),
                                NoopRetentionStrategyConfig.create(25),
                                ZonedDateTime.of(2016, 10, 4, 18, 0, 0, 0, ZoneOffset.UTC),
                                "standard",
                                "test_2",
                                null,
                                1,
                                false
                        ),
                        IndexSetConfig.create(
                                "57f3d721a43c2d59cb750003",
                                "Test 3",
                                "This is the index set configuration for Test 3 - with an index set index template",
                                true,
                                "test_3",
                                1,
                                0,
                                MessageCountRotationStrategy.class.getCanonicalName(),
                                MessageCountRotationStrategyConfig.create(2500),
                                NoopRetentionStrategy.class.getCanonicalName(),
                                NoopRetentionStrategyConfig.create(25),
                                ZonedDateTime.of(2016, 10, 4, 18, 0, 0, 0, ZoneOffset.UTC),
                                "standard",
                                "test_3",
                                IndexSetConfig.TemplateType.EVENTS,
                                1,
                                false
                        )
                );
    }

    @Test
    public void save() throws Exception {
        final IndexSetCreatedSubscriber subscriber = new IndexSetCreatedSubscriber();
        clusterEventBus.registerClusterEventSubscriber(subscriber);
        final IndexSetConfig indexSetConfig = IndexSetConfig.create(
                "Test 3",
                null,
                true,
                "test_3",
                10,
                0,
                MessageCountRotationStrategy.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(10000),
                NoopRetentionStrategy.class.getCanonicalName(),
                NoopRetentionStrategyConfig.create(5),
                ZonedDateTime.of(2016, 10, 4, 12, 0, 0, 0, ZoneOffset.UTC),
                "standard",
                "index-template",
                IndexSetConfig.TemplateType.EVENTS,
                1,
                false
        );

        final IndexSetConfig savedIndexSetConfig = indexSetService.save(indexSetConfig);

        final Optional<IndexSetConfig> retrievedIndexSetConfig = indexSetService.get(savedIndexSetConfig.id());
        assertThat(retrievedIndexSetConfig)
                .isPresent()
                .contains(savedIndexSetConfig);
        assertThat(subscriber.getEvents())
                .hasSize(1)
                .containsExactly(IndexSetCreatedEvent.create(savedIndexSetConfig));
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void deleteWithStringId() throws Exception {
        final IndexSetDeletedSubscriber subscriber = new IndexSetDeletedSubscriber();
        clusterEventBus.registerClusterEventSubscriber(subscriber);

        final int deletedEntries = indexSetService.delete("57f3d721a43c2d59cb750001");
        assertThat(deletedEntries).isEqualTo(1);
        assertThat(indexSetService.get("57f3d721a43c2d59cb750001")).isEmpty();

        assertThat(subscriber.getEvents())
                .hasSize(1)
                .containsExactly(IndexSetDeletedEvent.create("57f3d721a43c2d59cb750001"));
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void deleteRemovesExistingIndexSetConfig() throws Exception {
        final IndexSetDeletedSubscriber subscriber = new IndexSetDeletedSubscriber();
        clusterEventBus.registerClusterEventSubscriber(subscriber);

        final int deletedEntries = indexSetService.delete(new ObjectId("57f3d721a43c2d59cb750001"));
        assertThat(deletedEntries).isEqualTo(1);
        assertThat(indexSetService.get("57f3d721a43c2d59cb750001")).isEmpty();

        assertThat(subscriber.getEvents())
                .hasSize(1)
                .containsExactly(IndexSetDeletedEvent.create("57f3d721a43c2d59cb750001"));
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void deleteDoesNothingIfIndexSetConfigDoesNotExist() throws Exception {
        final IndexSetDeletedSubscriber subscriber = new IndexSetDeletedSubscriber();
        clusterEventBus.registerClusterEventSubscriber(subscriber);

        final int deletedEntries = indexSetService.delete("57f3d721a43c2d59cb750009");
        assertThat(deletedEntries).isEqualTo(0);
        assertThat(indexSetService.get("57f3d721a43c2d59cb750001")).isPresent();
        assertThat(indexSetService.get("57f3d721a43c2d59cb750009")).isEmpty();
        assertThat(indexSetService.findAll()).hasSize(3);

        assertThat(subscriber.getEvents()).isEmpty();
    }

    @Test
    @MongoDBFixtures("MongoIndexSetServiceTest.json")
    public void deleteWithAssignedStreams() throws Exception {
        final IndexSetDeletedSubscriber subscriber = new IndexSetDeletedSubscriber();
        clusterEventBus.registerClusterEventSubscriber(subscriber);

        final FakeStream stream1 = new FakeStream("Test stream 1");

        final String streamId = "57f3d721a43c2d59cb750001";
        stream1.setIndexSetId(streamId);

        when(streamService.loadAllWithIndexSet(streamId)).thenReturn(Collections.singletonList(stream1));

        final int deletedEntries = indexSetService.delete(streamId);
        assertThat(deletedEntries).isEqualTo(0);
        assertThat(indexSetService.get(streamId)).isPresent();
        assertThat(indexSetService.findAll()).hasSize(3);

        assertThat(subscriber.getEvents()).isEmpty();
    }

    private static class IndexSetCreatedSubscriber {
        private final List<IndexSetCreatedEvent> events = new CopyOnWriteArrayList<>();

        @Subscribe
        public void createdEvent(IndexSetCreatedEvent event) {
            events.add(event);
        }

        public List<IndexSetCreatedEvent> getEvents() {
            return events;
        }
    }

    private static class IndexSetDeletedSubscriber {
        private final List<IndexSetDeletedEvent> events = new CopyOnWriteArrayList<>();

        @Subscribe
        public void createdEvent(IndexSetDeletedEvent event) {
            events.add(event);
        }

        public List<IndexSetDeletedEvent> getEvents() {
            return events;
        }
    }
}
