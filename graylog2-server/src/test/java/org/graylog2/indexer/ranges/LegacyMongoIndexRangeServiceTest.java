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
package org.graylog2.indexer.ranges;

import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.NotFoundException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.SortedSet;

import static org.assertj.core.api.Assertions.assertThat;

public class LegacyMongoIndexRangeServiceTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private static final DateTime EPOCH = new DateTime(0L, DateTimeZone.UTC);

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private LegacyMongoIndexRangeService indexRangeService;

    @Before
    public void setUp() throws Exception {
        indexRangeService = new LegacyMongoIndexRangeService(mongodb.mongoConnection());
    }

    @Test
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testGetExistingIndexRange() throws Exception {
        final IndexRange indexRange = indexRangeService.get("graylog_0");
        final DateTime end = new DateTime(2015, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        final IndexRange expected = MongoIndexRange.create(new ObjectId("56250da2d400000000000001"), "graylog_0", EPOCH, end, end, 0);
        assertThat(indexRange).isEqualTo(expected);
    }

    @Test(expected = NotFoundException.class)
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testGetNonExistingIndexRange() throws Exception {
        indexRangeService.get("does-not-exist");
    }

    @Test(expected = NotFoundException.class)
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testGetInvalidIndexRange() throws Exception {
        indexRangeService.get("invalid");
    }

    @Test
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testGetIncompleteIndexRange() throws Exception {
        final IndexRange indexRange = indexRangeService.get("graylog_99");
        final DateTime end = new DateTime(2015, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        final IndexRange expected = MongoIndexRange.create(new ObjectId("56250da2d400000000000099"), "graylog_99", EPOCH, end, EPOCH, 0);
        assertThat(indexRange).isEqualTo(expected);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testFind() throws Exception {
        indexRangeService.find(new DateTime(0L, DateTimeZone.UTC), DateTime.now(DateTimeZone.UTC));
    }

    @Test
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testFindAll() throws Exception {
        final SortedSet<IndexRange> indexRanges = indexRangeService.findAll();

        final DateTime end0 = new DateTime(2015, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        final DateTime end1 = new DateTime(2015, 1, 2, 0, 0, 0, 0, DateTimeZone.UTC);
        final DateTime end2 = new DateTime(2015, 1, 3, 0, 0, 0, 0, DateTimeZone.UTC);
        final DateTime end99 = new DateTime(2015, 1, 1, 0, 0, 0, 0, DateTimeZone.UTC);
        assertThat(indexRanges).containsExactly(
                MongoIndexRange.create(new ObjectId("56250da2d400000000000001"), "graylog_0", EPOCH, end0, end0, 0),
                MongoIndexRange.create(new ObjectId("56250da2d400000000000099"), "graylog_99", EPOCH, end99, EPOCH, 0),
                MongoIndexRange.create(new ObjectId("56250da2d400000000000002"), "graylog_1", EPOCH, end1, end1, 1),
                MongoIndexRange.create(new ObjectId("56250da2d400000000000003"), "graylog_2", EPOCH, end2, end2, 2)
        );
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSave() throws Exception {
        indexRangeService.save((IndexRange) null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCalculateRange() throws Exception {
        indexRangeService.calculateRange("graylog_0");
    }

    @Test
    @MongoDBFixtures("LegacyMongoIndexRangeServiceTest.json")
    public void testDelete() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(4);

        indexRangeService.delete("graylog_1");

        assertThat(indexRangeService.findAll()).hasSize(3);
    }
}
