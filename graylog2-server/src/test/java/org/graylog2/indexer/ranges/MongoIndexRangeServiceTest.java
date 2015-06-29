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
package org.graylog2.indexer.ranges;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import org.assertj.jodatime.api.Assertions;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.shared.system.activities.NullActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;
import java.util.SortedSet;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MongoIndexRangeServiceTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Mock
    private Searches searches;
    private MongoIndexRangeService indexRangeService;

    @Before
    public void setUp() throws Exception {
        indexRangeService = new MongoIndexRangeService(mongoRule.getMongoConnection(), new NullActivityWriter(), searches);
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getReturnsExistingIndexRange() throws Exception {
        IndexRange indexRange = indexRangeService.get("graylog_1");

        assertThat(indexRange.getIndexName()).isEqualTo("graylog_1");
        assertThat(indexRange.getStart()).isEqualTo(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.getCalculatedAt()).isEqualTo(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.getCalculationTookMs()).isEqualTo(23);
    }

    @Test(expected = NotFoundException.class)
    public void getThrowsNotFoundException() throws Exception {
        indexRangeService.get("does-not-exist");
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getFromReturnsIndexRangesAfterTimestamp() throws Exception {
        final long millis = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).getMillis();
        SortedSet<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

        assertThat(indexRanges).hasSize(2);
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getFromReturnsNothingBeforeTimestamp() throws Exception {
        final long millis = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC).getMillis();
        Set<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

        assertThat(indexRanges).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getFromWithDateTimeReturnsIndexRangesAfterTimestamp() throws Exception {
        final DateTime dateTime = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        SortedSet<IndexRange> indexRanges = indexRangeService.getFrom(dateTime);

        assertThat(indexRanges).hasSize(2);
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getFromWithDateTimeReturnsNothingBeforeTimestamp() throws Exception {
        final DateTime dateTime = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        Set<IndexRange> indexRanges = indexRangeService.getFrom(dateTime);

        assertThat(indexRanges).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyRemovesIndexRange() throws Exception {
        indexRangeService.destroy("graylog_1");

        Set<IndexRange> indexRanges = indexRangeService.getFrom(0);

        assertThat(indexRanges).hasSize(1);
        assertThat(indexRanges.iterator().next().getIndexName()).isEqualTo("graylog_2");
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyRemovesIgnoresNonExistingIndexRange() throws Exception {
        indexRangeService.destroy("does-not-exist");

        final long millis = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).getMillis();
        Set<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

        assertThat(indexRanges).hasSize(2);
    }

    @Test
    public void createReturnsIndexRange() throws Exception {
        final DateTime dateTime = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final int timestamp = Ints.saturatedCast(dateTime.getMillis() / 1000L);
        IndexRange indexRange = indexRangeService.create(ImmutableMap.<String, Object>of(
                        "index", "graylog_3",
                        "start", timestamp,
                        "calculated_at", timestamp,
                        "took_ms", 42
                )
        );

        assertThat(indexRange.getIndexName()).isEqualTo("graylog_3");
        assertThat(indexRange.getStart()).isEqualTo(dateTime);
        assertThat(indexRange.getCalculatedAt()).isEqualTo(dateTime);
        assertThat(indexRange.getCalculationTookMs()).isEqualTo(42);
    }

    @Test
    public void calculateRangeReturnsIndexRange() throws Exception {
        final String index = "graylog_test";
        final DateTime dateTime = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        when(searches.findNewestMessageTimestampOfIndex(index)).thenReturn(dateTime);

        final IndexRange indexRange = indexRangeService.calculateRange(index);

        assertThat(indexRange.getIndexName()).isEqualTo(index);
        assertThat(indexRange.getStart()).isEqualTo(dateTime);
        Assertions.assertThat(indexRange.getCalculatedAt()).isEqualToIgnoringHours(DateTime.now(DateTimeZone.UTC));
    }

    @Test
    public void testCalculateRangeWithEmptyIndex() throws Exception {
        final String index = "graylog_test";
        when(searches.findNewestMessageTimestampOfIndex(index)).thenReturn(null);
        final IndexRange range = indexRangeService.calculateRange(index);

        assertThat(range).isNotNull();
        assertThat(range.getIndexName()).isEqualTo(index);
        Assertions.assertThat(range.getStart()).isEqualToIgnoringHours(DateTime.now(DateTimeZone.UTC));
    }

    @Test(expected = IndexMissingException.class)
    public void testCalculateRangeWithNonExistingIndex() throws Exception {
        when(searches.findNewestMessageTimestampOfIndex("does-not-exist")).thenThrow(IndexMissingException.class);
        indexRangeService.calculateRange("does-not-exist");
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyAllKillsAllIndexRanges() throws Exception {
        indexRangeService.destroyAll();

        assertThat(indexRangeService.getFrom(0)).isEmpty();
    }

    @Test
    public void savePersistsIndexRange() throws Exception {
        final DateTime dateTime = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final int timestamp = Ints.saturatedCast(dateTime.getMillis() / 1000L);
        final IndexRange indexRange = indexRangeService.create(ImmutableMap.<String, Object>of(
                        "index", "graylog_test",
                        "start", timestamp,
                        "calculated_at", timestamp,
                        "took_ms", 42
                )
        );

        indexRangeService.save(indexRange);

        final DBCollection collection = mongoRule.getMongoConnection().getDatabase().getCollection("index_ranges");
        final DBObject query = new BasicDBObject("index", "graylog_test");
        final DBObject result = collection.findOne(query);

        assertThat(result.get("index")).isEqualTo("graylog_test");
        assertThat(result.get("start")).isEqualTo(timestamp);
        assertThat(result.get("calculated_at")).isEqualTo(timestamp);
        assertThat(result.get("took_ms")).isEqualTo(42);
    }
}