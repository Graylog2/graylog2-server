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

import autovalue.shaded.com.google.common.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.database.NotFoundException;
import org.graylog2.shared.system.activities.NullActivityWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;

public class IndexRangeServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    private IndexRangeServiceImpl indexRangeService;

    @Before
    public void setUp() throws Exception {
        indexRangeService = new IndexRangeServiceImpl(mongoRule.getMongoConnection(), new NullActivityWriter());
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
        List<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

        assertThat(indexRanges)
                .hasSize(2)
                .isSortedAccordingTo(new IndexRangeComparator());
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getFromReturnsNothingBeforeTimestamp() throws Exception {
        final long millis = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC).getMillis();
        List<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

        assertThat(indexRanges).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyRemovesIndexRange() throws Exception {
        indexRangeService.destroy("graylog_1");

        List<IndexRange> indexRanges = indexRangeService.getFrom(0);

        assertThat(indexRanges).hasSize(1);
        assertThat(indexRanges.get(0).getIndexName()).isEqualTo("graylog_2");
    }

    @Test
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyRemovesIgnoresNonExistingIndexRange() throws Exception {
        indexRangeService.destroy("does-not-exist");

        final long millis = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC).getMillis();
        List<IndexRange> indexRanges = indexRangeService.getFrom(Ints.saturatedCast(millis / 1000L));

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
    @UsingDataSet(locations = "IndexRangeServiceImplTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void destroyAllKillsAllIndexRanges() throws Exception {
        indexRangeService.destroyAll();

        assertThat(indexRangeService.getFrom(0)).isEmpty();
    }
}