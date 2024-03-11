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

    private static final String PROFILE1_ID = "aa0000000000000000000001";
    private static final String PROFILE2_ID = "aa0000000000000000000002";
    private static final String UNUSED_PROFILE_ID = "dada00000000000000000000";
    private static final String WRONG_PROFILE_ID = "that's not proper ObjetID!";
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
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000001", PROFILE1_ID));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000011", PROFILE1_ID));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000002", PROFILE2_ID));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000042", null));
        toTest = new IndexFieldTypeProfileUsagesService(mongoConnection);
    }

    @Test
    public void testReturnsProperUsagesForSingleProfile() {
        assertEquals(Set.of("000000000000000000000001", "000000000000000000000011"),
                toTest.usagesOfProfile(PROFILE1_ID));
        assertEquals(Set.of("000000000000000000000002"),
                toTest.usagesOfProfile(PROFILE2_ID));
        assertEquals(Set.of(),
                toTest.usagesOfProfile(UNUSED_PROFILE_ID));
        assertEquals(Set.of(),
                toTest.usagesOfProfile(WRONG_PROFILE_ID));
    }

    @Test
    public void testReturnsProperUsagesForMultipleProfiles() {
        Map<String, Set<String>> expectedResult = Map.of(
                PROFILE1_ID, Set.of("000000000000000000000001", "000000000000000000000011"),
                PROFILE2_ID, Set.of("000000000000000000000002"),
                UNUSED_PROFILE_ID, Set.of(),
                WRONG_PROFILE_ID, Set.of()
        );

        assertEquals(expectedResult, toTest.usagesOfProfiles(Set.of(PROFILE1_ID, PROFILE2_ID, UNUSED_PROFILE_ID, WRONG_PROFILE_ID)));
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
                profileId,
                null);
    }

}
