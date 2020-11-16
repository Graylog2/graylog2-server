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
package org.graylog2.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.DefaultIndexSetConfig;
import org.graylog2.migrations.V20161215163900_MoveIndexSetDefaultConfig.MigrationCompleted;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.ZonedDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class V20161215163900_MoveIndexSetDefaultConfigTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    public NodeId nodeId;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(objectMapper);

    private ClusterConfigServiceImpl clusterConfigService;
    private Migration migration;
    private MongoCollection<Document> collection;

    @Before
    public void setUp() throws Exception {
        this.clusterConfigService = spy(new ClusterConfigServiceImpl(objectMapperProvider,
                mongodb.mongoConnection(), nodeId,
                new ChainingClassLoader(getClass().getClassLoader()), new ClusterEventBus()));

        this.collection = mongodb.mongoConnection().getMongoDatabase().getCollection("index_sets");

        this.migration = new V20161215163900_MoveIndexSetDefaultConfig(mongodb.mongoConnection(), clusterConfigService);
    }

    @Test
    public void createdAt() throws Exception {
        assertThat(migration.createdAt()).isEqualTo(ZonedDateTime.parse("2016-12-15T16:39:00Z"));
    }

    @Test
    @MongoDBFixtures("V20161215163900_MoveIndexSetDefaultConfigTest.json")
    public void upgrade() throws Exception {
        final long count = collection.count();

        migration.upgrade();

        final MigrationCompleted migrationCompleted = clusterConfigService.get(MigrationCompleted.class);

        assertThat(collection.count())
                .withFailMessage("No document should be deleted by the migration!")
                .isEqualTo(count);
        assertThat(collection.count(Filters.exists("default")))
                .withFailMessage("The migration should have deleted the \"default\" field from the documents!")
                .isEqualTo(0L);

        assertThat(clusterConfigService.get(DefaultIndexSetConfig.class))
                .withFailMessage("The DefaultIndexSetConfig should have been written to cluster config!")
                .isNotNull();
        assertThat(clusterConfigService.get(DefaultIndexSetConfig.class).defaultIndexSetId()).isEqualTo("57f3d721a43c2d59cb750001");

        assertThat(migrationCompleted).isNotNull();
        assertThat(migrationCompleted.indexSetIds()).containsExactlyInAnyOrder("57f3d721a43c2d59cb750001", "57f3d721a43c2d59cb750003");
    }

    @Test
    @MongoDBFixtures("V20161215163900_MoveIndexSetDefaultConfigTest.json")
    public void upgradeWhenMigrationCompleted() throws Exception {
        // Count how many documents with a "default" field are in the database.
        final long count = collection.count(Filters.exists("default"));

        assertThat(count)
                .withFailMessage("There should be at least one document with a \"default\" field in the database")
                .isGreaterThan(0L);

        clusterConfigService.write(MigrationCompleted.create(Collections.emptySet()));
        migration.upgrade();

        // If the MigrationCompleted object has been written to the cluster config, the migration shouldn't do anything
        // and shouldn't touch the database. Thank means we should still have all documents with the "default" field
        // from the seed file in the database.
        assertThat(collection.count(Filters.exists("default"))).isEqualTo(count);
    }

    @Test
    public void upgradeWhenDefaultIndexSetConfigExists() throws Exception {
        clusterConfigService.write(DefaultIndexSetConfig.create("57f3d721a43c2d59cb750001"));

        // Reset the spy to be able to verify that there wasn't a write
        reset(clusterConfigService);

        migration.upgrade();

        verify(clusterConfigService, never()).write(any(DefaultIndexSetConfig.class));
    }
}
