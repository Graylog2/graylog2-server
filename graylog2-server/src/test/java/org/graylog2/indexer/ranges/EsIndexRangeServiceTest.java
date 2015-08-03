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

import com.google.common.collect.ImmutableSet;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch;
import org.assertj.jodatime.api.Assertions;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.indices.IndexMissingException;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.nosqlunit.IndexCreatingLoadStrategyFactory;
import org.graylog2.indexer.searches.TimestampStats;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.Set;

import static com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

@RunWith(MockitoJUnitRunner.class)
public class EsIndexRangeServiceTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule()
            .settings(ImmutableSettings.settingsBuilder().put("action.auto_create_index", false).build())
            .build();
    private static final ImmutableSet<String> INDEX_NAMES = ImmutableSet.of("graylog", "graylog_1", "graylog_2");
    private static final ElasticsearchConfiguration ELASTICSEARCH_CONFIGURATION = new ElasticsearchConfiguration() {
        @Override
        public String getIndexPrefix() {
            return "graylog";
        }
    };

    @Rule
    public ElasticsearchRule elasticsearchRule;

    @Inject
    private Client client;
    private Indices indices;

    private EsIndexRangeService indexRangeService;

    public EsIndexRangeServiceTest() {
        this.elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
        this.elasticsearchRule.setLoadStrategyFactory(
                new IndexCreatingLoadStrategyFactory(INDEX_NAMES, ELASTICSEARCH_CONFIGURATION));
    }

    @Before
    public void setUp() throws Exception {
        indices = new Indices(client, ELASTICSEARCH_CONFIGURATION, new IndexMapping(client));
        indexRangeService = new EsIndexRangeService(client, new ObjectMapperProvider().get(), indices);
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void getReturnsExistingIndexRange() throws Exception {
        IndexRange indexRange = indexRangeService.get("graylog_1");

        assertThat(indexRange.indexName()).isEqualTo("graylog_1");
        assertThat(indexRange.begin()).isEqualTo(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.end()).isEqualTo(new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.calculatedAt()).isEqualTo(new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC));
        assertThat(indexRange.calculationDuration()).isEqualTo(23);
    }

    @Test(expected = NotFoundException.class)
    public void getThrowsNotFoundException() throws Exception {
        indexRangeService.get("does-not-exist");
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findReturnsIndexRangesWithinGivenRange() throws Exception {
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        Set<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findReturnsNothingBeforeBegin() throws Exception {
        final DateTime begin = new DateTime(2016, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2016, 1, 2, 0, 0, DateTimeZone.UTC);
        Set<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).isEmpty();
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findAllReturnsAllIndexRanges() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(2);
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void calculateRangeReturnsIndexRange() throws Exception {
        final String index = "graylog";
        final DateTime min = new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC);
        final DateTime max = new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC);
        final IndexRange indexRange = indexRangeService.calculateRange(index);

        assertThat(indexRange.indexName()).isEqualTo(index);
        assertThat(indexRange.begin()).isEqualTo(min);
        assertThat(indexRange.end()).isEqualTo(max);
        Assertions.assertThat(indexRange.calculatedAt()).isEqualToIgnoringHours(DateTime.now(DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCalculateRangeWithEmptyIndex() throws Exception {
        final String index = "graylog";
        final IndexRange range = indexRangeService.calculateRange(index);

        assertThat(range).isNotNull();
        assertThat(range.indexName()).isEqualTo(index);
        assertThat(range.begin()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(range.end()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexMissingException.class)
    public void testCalculateRangeWithNonExistingIndex() throws Exception {
        indexRangeService.calculateRange("does-not-exist");
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void savePersistsIndexRange() throws Exception {
        final String indexName = "graylog";
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = IndexRange.create(indexName, begin, end, now, 42);

        indexRangeService.save(indexRange);

        final IndexRange result = indexRangeService.get(indexName);
        assertThat(result.indexName()).isEqualTo(indexName);
        assertThat(result.begin()).isEqualTo(begin);
        assertThat(result.end()).isEqualTo(end);
        assertThat(result.calculatedAt()).isEqualTo(now);
        assertThat(result.calculationDuration()).isEqualTo(42);
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void savePersistsIndexRangeInReadOnlyIndex() throws Exception {
        final String indexName = "graylog_read_only";
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRange = IndexRange.create(indexName, begin, end, now, 42);

        try {
            indices.create(indexName);
            indices.setReadOnly(indexName);
            assumeTrue(indices.isReadOnly(indexName));
            indexRangeService.save(indexRange);

            assertThat(indices.isReadOnly(indexName)).isTrue();

            final IndexRange result = indexRangeService.get(indexName);
            assertThat(result.indexName()).isEqualTo(indexName);
            assertThat(result.begin()).isEqualTo(begin);
            assertThat(result.end()).isEqualTo(end);
            assertThat(result.calculatedAt()).isEqualTo(now);
            assertThat(result.calculationDuration()).isEqualTo(42);
        } finally {
            indices.delete(indexName);
        }
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.DELETE_ALL)
    public void saveOverwritesExistingIndexRange() throws Exception {
        final String indexName = "graylog";
        final DateTime begin = new DateTime(2015, 1, 1, 0, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final IndexRange indexRangeBefore = IndexRange.create(indexName, begin, end, now, 1);
        final IndexRange indexRangeAfter = IndexRange.create(indexName, begin, end, now, 2);

        indexRangeService.save(indexRangeBefore);

        final IndexRange before = indexRangeService.get(indexName);
        assertThat(before.calculationDuration()).isEqualTo(1);

        indexRangeService.save(indexRangeAfter);

        final IndexRange after = indexRangeService.get(indexName);
        assertThat(after.calculationDuration()).isEqualTo(2);
    }


    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTimestampStatsOfIndex() throws Exception {
        TimestampStats stats = indexRangeService.timestampStatsOfIndex("graylog");

        assertThat(stats.min()).isEqualTo(new DateTime(2015, 1, 1, 1, 0, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(2015, 1, 1, 5, 0, DateTimeZone.UTC));
        assertThat(stats.avg()).isEqualTo(new DateTime(2015, 1, 1, 3, 0, DateTimeZone.UTC));
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest-EmptyIndex.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testTimestampStatsOfIndexWithEmptyIndex() throws Exception {
        TimestampStats stats = indexRangeService.timestampStatsOfIndex("graylog");

        assertThat(stats.min()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.max()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
        assertThat(stats.avg()).isEqualTo(new DateTime(0L, DateTimeZone.UTC));
    }

    @Test(expected = IndexMissingException.class)
    public void testTimestampStatsOfIndexWithNonExistingIndex() throws Exception {
        indexRangeService.timestampStatsOfIndex("does-not-exist");
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteRemovesIndexRange() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(2);

        assertThat(indexRangeService.delete("graylog_1")).isTrue();

        assertThat(indexRangeService.findAll()).hasSize(1);
    }

    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void deleteHandlesInvalidIndexNames() throws Exception {
        assertThat(indexRangeService.findAll()).hasSize(2);

        assertThat(indexRangeService.delete("ignore_me")).isFalse();

        assertThat(indexRangeService.findAll()).hasSize(2);
    }
}