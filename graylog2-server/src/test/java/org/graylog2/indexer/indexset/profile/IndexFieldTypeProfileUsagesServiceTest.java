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
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
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

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class IndexFieldTypeProfileUsagesServiceTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private IndexFieldTypeProfileUsagesService toTest;

    @Before
    public void setUp() {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        final MongoIndexSetService mongoIndexSetService = new MongoIndexSetService(mongoConnection,
                objectMapperProvider,
                mock(StreamService.class),
                mock(ClusterConfigService.class),
                mock(ClusterEventBus.class)
        );
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000001", "profile1"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000011", "profile1"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000002", "profile2"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000042", null));
        toTest = new IndexFieldTypeProfileUsagesService(mongoConnection);
    }

    @Test
    public void testReturnsProperUsagesForSingleProfile() {
        assertEquals(Set.of("000000000000000000000001", "000000000000000000000011"),
                toTest.usagesOfProfile("profile1"));
        assertEquals(Set.of("000000000000000000000002"),
                toTest.usagesOfProfile("profile2"));
        assertEquals(Set.of(),
                toTest.usagesOfProfile("unused_profile"));
    }

    @Test
    public void testReturnsProperUsagesForMultipleProfiles() {
        Map<String, Set<String>> expectedResult = Map.of(
                "profile1", Set.of("000000000000000000000001", "000000000000000000000011"),
                "profile2", Set.of("000000000000000000000002"),
                "unused_profile", Set.of()
        );

        assertEquals(expectedResult, toTest.usagesOfProfiles(Set.of("profile1", "profile2", "unused_profile")));
    }

    private IndexSetConfig createIndexSetConfigForTest(final String id, final String profileId) {
        return IndexSetConfig.create(
                id, "title", "description",
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
                profileId);
    }

}
