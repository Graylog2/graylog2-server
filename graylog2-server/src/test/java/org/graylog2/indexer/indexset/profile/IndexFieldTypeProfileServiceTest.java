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
package org.graylog2.indexer.indexset.profile;

import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.CommonMongoJackObjectMapperProvider;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class IndexFieldTypeProfileServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private IndexFieldTypeProfileService toTest;

    private IndexFieldTypeProfileUsagesService indexFieldTypeProfileUsagesService;

    private MongoIndexSetService mongoIndexSetService;

    @Before
    public void setUp() {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        mongoIndexSetService = new MongoIndexSetService(mongoConnection,
                objectMapperProvider,
                mock(StreamService.class),
                mock(ClusterConfigService.class),
                mock(ClusterEventBus.class)
        );
        indexFieldTypeProfileUsagesService = Mockito.mock(IndexFieldTypeProfileUsagesService.class);
        toTest = new IndexFieldTypeProfileService(mongoConnection,
                objectMapperProvider,
                new MongoCollections(new CommonMongoJackObjectMapperProvider(objectMapperProvider), mongoConnection),
                indexFieldTypeProfileUsagesService,
                mongoIndexSetService);
    }

    @Test
    public void testRemovalOfProfileRemovesItsUsagesInIndexSet() {
        toTest.save(new IndexFieldTypeProfile("123400000000000000000001", "profile1", "profile1", new CustomFieldMappings()));
        toTest.save(new IndexFieldTypeProfile("123400000000000000000002", "profile2", "profile2", new CustomFieldMappings()));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000001",
                "Index set using profile 1",
                "123400000000000000000001"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000011",
                "Another Index set using profile 1",
                "123400000000000000000001"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000002",
                "Index set using profile 2",
                "123400000000000000000002"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000042",
                "Index set without profile",
                null));

        toTest.delete("123400000000000000000001");

        verifyHasNoProfile("000000000000000000000001");//check if profile removed
        verifyHasNoProfile("000000000000000000000011");//check if profile removed
        verifyHasNoProfile("000000000000000000000042");//check if profile still empty, as before

        verifyHasProfile("000000000000000000000002", "123400000000000000000002");//check if other profile usages not affected

    }

    private void verifyHasNoProfile(final String indexSetId) {
        Optional<IndexSetConfig> indexSetConfig = mongoIndexSetService.get(indexSetId);
        assertTrue(indexSetConfig.isPresent());
        assertNull(indexSetConfig.get().fieldTypeProfile());
    }

    private void verifyHasProfile(final String indexSetId, final String profileId) {
        Optional<IndexSetConfig> indexSetConfig = mongoIndexSetService.get(indexSetId);
        assertTrue(indexSetConfig.isPresent());
        assertEquals(profileId, indexSetConfig.get().fieldTypeProfile());
    }


    private IndexSetConfig createIndexSetConfigForTest(final String id, final String description, final String profileId) {
        return IndexSetConfig.create(
                id, "title", description,
                true,
                true, "prefix_" + id, null, null,
                1, 0,
                MessageCountRotationStrategyConfig.class.getCanonicalName(),
                MessageCountRotationStrategyConfig.create(3),
                DeletionRetentionStrategy.class.getCanonicalName(),
                DeletionRetentionStrategyConfig.createDefault(),
                ZonedDateTime.now(ZoneId.systemDefault()),
                null, null, null,
                1, true,
                Duration.standardSeconds(5),
                new CustomFieldMappings(),
                profileId,
                null);
    }

}
