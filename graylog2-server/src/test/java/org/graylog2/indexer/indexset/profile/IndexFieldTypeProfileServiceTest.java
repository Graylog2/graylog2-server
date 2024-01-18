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
import org.graylog2.indexer.indexset.CustomFieldMapping;
import org.graylog2.indexer.indexset.CustomFieldMappings;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamService;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        indexFieldTypeProfileUsagesService = new IndexFieldTypeProfileUsagesService(mongoConnection);
        toTest = new IndexFieldTypeProfileService(mongoConnection,
                objectMapperProvider,
                new MongoCollections(new CommonMongoJackObjectMapperProvider(objectMapperProvider), mongoConnection),
                indexFieldTypeProfileUsagesService,
                mongoIndexSetService);
    }

    @Test
    public void testRetrievalWithUsages() {
        final IndexFieldTypeProfile profile1 = new IndexFieldTypeProfile("123400000000000000000001", "profile1", "profile1", new CustomFieldMappings());
        toTest.save(profile1);
        final IndexFieldTypeProfile profile2 = new IndexFieldTypeProfile("123400000000000000000002", "profile2", "profile2", new CustomFieldMappings());
        toTest.save(profile2);
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000001",
                "Index set using profile 1",
                "123400000000000000000001"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000011",
                "Another Index set using profile 1",
                "123400000000000000000001"));
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000002",
                "Index set using profile 2",
                "123400000000000000000002"));


        final Optional<IndexFieldTypeProfileWithUsages> profile1WithUsages = toTest.getWithUsages("123400000000000000000001");
        assertTrue(profile1WithUsages.isPresent());
        assertEquals(
                new IndexFieldTypeProfileWithUsages(profile1, Set.of("000000000000000000000001", "000000000000000000000011")),
                profile1WithUsages.get()
        );
        final Optional<IndexFieldTypeProfileWithUsages> profile2WithUsages = toTest.getWithUsages("123400000000000000000002");
        assertTrue(profile2WithUsages.isPresent());
        assertEquals(
                new IndexFieldTypeProfileWithUsages(profile2, Set.of("000000000000000000000002")),
                profile2WithUsages.get()
        );
    }

    @Test
    public void testUpdate() {
        final IndexFieldTypeProfile profile = new IndexFieldTypeProfile("123400000000000000000001", "profile", "profile", new CustomFieldMappings());
        toTest.save(profile);

        final IndexFieldTypeProfile updatedProfile = new IndexFieldTypeProfile(profile.id(), "Changed!", "Changed!",
                new CustomFieldMappings(List.of(new CustomFieldMapping("field", "date"))));
        toTest.update(profile.id(), updatedProfile);

        assertEquals(updatedProfile, toTest.get(profile.id()).get());
    }

    @Test
    public void testAllProfilesRetrieval() {
        final IndexFieldTypeProfile profile1 = new IndexFieldTypeProfile("123400000000000000000001", "xx", "xxxxxx", new CustomFieldMappings());
        toTest.save(profile1);
        final IndexFieldTypeProfile profile2 = new IndexFieldTypeProfile("123400000000000000000002", "aa", "aaaaaa", new CustomFieldMappings());
        toTest.save(profile2);
        final IndexFieldTypeProfile profile3 = new IndexFieldTypeProfile("123400000000000000000003", "ax", "aaaxxxxx", new CustomFieldMappings());
        toTest.save(profile3);

        final List<IndexFieldTypeProfileIdAndName> all = toTest.getAll();

        assertEquals(List.of(
                        new IndexFieldTypeProfileIdAndName("123400000000000000000002", "aa"),
                        new IndexFieldTypeProfileIdAndName("123400000000000000000003", "ax"),
                        new IndexFieldTypeProfileIdAndName("123400000000000000000001", "xx")
                ),
                all);
    }

    @Test
    public void testPagination() {
        final IndexFieldTypeProfile profile1 = new IndexFieldTypeProfile("123400000000000000000001", "a", "aa", new CustomFieldMappings());
        toTest.save(profile1);
        final IndexFieldTypeProfile profile2 = new IndexFieldTypeProfile("123400000000000000000002", "b", "ab", new CustomFieldMappings());
        toTest.save(profile2);
        final IndexFieldTypeProfile profile3 = new IndexFieldTypeProfile("123400000000000000000003", "c", "c", new CustomFieldMappings());
        toTest.save(profile3);
        mongoIndexSetService.save(createIndexSetConfigForTest("000000000000000000000001",
                "Index set using profile 1",
                profile1.id()));


        PageListResponse<IndexFieldTypeProfileWithUsages> paginatedResponse = toTest.getPaginated("", List.of(), 1, 2, "name", "asc");

        verifyPaginationResponse(
                paginatedResponse,
                List.of(
                        new IndexFieldTypeProfileWithUsages(profile1, Set.of("000000000000000000000001")),
                        new IndexFieldTypeProfileWithUsages(profile2, Set.of())
                ),
                3
        );

        paginatedResponse = toTest.getPaginated("", List.of(), 2, 2, "name", "asc");

        verifyPaginationResponse(
                paginatedResponse,
                List.of(
                        new IndexFieldTypeProfileWithUsages(profile3, Set.of())
                ),
                3
        );

        paginatedResponse = toTest.getPaginated("description:a", List.of(), 1, 2, "description", "desc");

        verifyPaginationResponse(
                paginatedResponse,
                List.of(
                        new IndexFieldTypeProfileWithUsages(profile2, Set.of()),
                        new IndexFieldTypeProfileWithUsages(profile1, Set.of("000000000000000000000001"))
                ),
                2
        );

        paginatedResponse = toTest.getPaginated("description:a", List.of("name:a"), 1, 2, "description", "desc");

        verifyPaginationResponse(
                paginatedResponse,
                List.of(
                        new IndexFieldTypeProfileWithUsages(profile1, Set.of("000000000000000000000001"))
                ),
                1
        );

    }

    private void verifyPaginationResponse(final PageListResponse<IndexFieldTypeProfileWithUsages> paginatedResponse,
                                          final List<IndexFieldTypeProfileWithUsages> expectedProfiles,
                                          final long total
    ) {
        assertEquals(expectedProfiles.size(), paginatedResponse.elements().size());
        assertEquals(expectedProfiles, paginatedResponse.elements());
        assertEquals(total, paginatedResponse.total());
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
        assertTrue(toTest.get("123400000000000000000001").isEmpty());

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
                profileId);
    }

}
