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

import com.codahale.metrics.MetricRegistry;
import com.google.common.primitives.Ints;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch;
import org.elasticsearch.client.Client;
import org.graylog2.Configuration;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.EmptyIndexException;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.SearchesTest;
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

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

import static com.lordofthejars.nosqlunit.elasticsearch.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RebuildIndexRangesJobTest {
    private static final String INDEX_NAME = "graylog";
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule().build();
    @Rule
    public ElasticsearchRule elasticsearchRule;

    @Mock
    private Deflector deflector;
    @Inject
    private Client client;
    @Mock
    private IndexRangeService indexRangeService;
    private RebuildIndexRangesJob rebuildIndexRangesJob;

    public RebuildIndexRangesJobTest() {
        this.elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
        this.elasticsearchRule.setLoadStrategyFactory(new SearchesTest.IndexCreatingLoadStrategyFactory(Collections.singleton(INDEX_NAME)));
    }

    @Before
    public void setUp() throws Exception {
        final MetricRegistry metricRegistry = new MetricRegistry();
        final Searches searches = new Searches(new Configuration(), deflector, indexRangeService, client, metricRegistry);
        rebuildIndexRangesJob = new RebuildIndexRangesJob(deflector, searches, new NullActivityWriter(), indexRangeService);
    }

    @Test
    @UsingDataSet(loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testCalculateRange() throws Exception {
        final Map<String, Object> range = rebuildIndexRangesJob.calculateRange(INDEX_NAME);

        assertThat(range).isNotNull();
        assertThat(range.get("index")).isEqualTo(INDEX_NAME);
        assertThat(range.get("start")).isEqualTo(Ints.saturatedCast(new DateTime(2015, 1, 1, 12, 0, DateTimeZone.UTC).getMillis() / 1000L));
    }

    @Test(expected = EmptyIndexException.class)
    public void testCalculateRangeWithNonExistingIndex() throws Exception {
        rebuildIndexRangesJob.calculateRange("does-not-exist");
    }
}