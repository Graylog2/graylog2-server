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
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule;
import com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.nosqlunit.IndexCreatingLoadStrategyFactory;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy;
import org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.SortedSet;

import static com.lordofthejars.nosqlunit.elasticsearch2.ElasticsearchRule.ElasticsearchRuleBuilder.newElasticsearchRule;
import static com.lordofthejars.nosqlunit.elasticsearch2.EmbeddedElasticsearch.EmbeddedElasticsearchRuleBuilder.newEmbeddedElasticsearchRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class EsIndexRangeServiceTest {
    @ClassRule
    public static final EmbeddedElasticsearch EMBEDDED_ELASTICSEARCH = newEmbeddedElasticsearchRule()
            .settings(Settings.settingsBuilder().put("action.auto_create_index", false).build())
            .build();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final ImmutableSet<String> INDEX_NAMES = ImmutableSet.of("graylog", "graylog_1", "graylog_2", "graylog_3", "graylog_4", "graylog_5", "ignored");

    private final IndexSetConfig indexSetConfig;

    @Rule
    public ElasticsearchRule elasticsearchRule;

    @Inject
    private Client client;
    @Mock
    private EventBus localEventBus;
    @Mock
    private AuditEventSender auditEventSender;
    @Mock
    private NodeId nodeId;
    @Mock
    private IndexSetRegistry indexSetRegistry;
    private EsIndexRangeService indexRangeService;

    public EsIndexRangeServiceTest() {
        this.indexSetConfig = IndexSetConfig.builder()
                .id("index-set-1")
                .title("Index set 1")
                .description("For testing")
                .indexPrefix("graylog")
                .creationDate(ZonedDateTime.now())
                .shards(1)
                .replicas(0)
                .rotationStrategyClass(MessageCountRotationStrategy.class.getCanonicalName())
                .rotationStrategy(MessageCountRotationStrategyConfig.createDefault())
                .retentionStrategyClass(DeletionRetentionStrategy.class.getCanonicalName())
                .retentionStrategy(DeletionRetentionStrategyConfig.createDefault())
                .build();
        this.elasticsearchRule = newElasticsearchRule().defaultEmbeddedElasticsearch();
        this.elasticsearchRule.setLoadStrategyFactory(new IndexCreatingLoadStrategyFactory(indexSetConfig, INDEX_NAMES));
    }

    @Before
    public void setUp() throws Exception {
        when(indexSetRegistry.getManagedIndicesNames()).thenReturn(new String[]{
                "graylog_1", "graylog_2", "graylog_3", "graylog_4", "graylog_5"});
       indexRangeService = new EsIndexRangeService(client, indexSetRegistry, localEventBus, new MetricRegistry());
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

    /**
     * Test the following constellation:
     * <pre>
     *                        [-        index range       -]
     * [- graylog_1 -][- graylog_2 -][- graylog_3 -][- graylog_4 -][- graylog_5 -]
     * </pre>
     */
    @Test
    @UsingDataSet(locations = "EsIndexRangeServiceTest-distinct.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void findReturnsIndexRangesWithinGivenRange() throws Exception {
        final DateTime begin = new DateTime(2015, 1, 2, 12, 0, DateTimeZone.UTC);
        final DateTime end = new DateTime(2015, 1, 4, 12, 0, DateTimeZone.UTC);
        final SortedSet<IndexRange> indexRanges = indexRangeService.find(begin, end);

        assertThat(indexRanges).containsExactly(
                EsIndexRange.create("graylog_2", new DateTime(2015, 1, 2, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), 42),
                EsIndexRange.create("graylog_3", new DateTime(2015, 1, 3, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), 42),
                EsIndexRange.create("graylog_4", new DateTime(2015, 1, 4, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 5, 0, 0, DateTimeZone.UTC), new DateTime(2015, 1, 5, 0, 0, DateTimeZone.UTC), 42)
        );
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

    @Test(expected = UnsupportedOperationException.class)
    public void calculateRangeReturnsIndexRange() throws Exception {
        indexRangeService.calculateRange("graylog");
    }
}
