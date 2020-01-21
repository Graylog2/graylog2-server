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
package org.graylog.events.processor.aggregation;

import com.google.common.collect.ImmutableList;
import org.graylog.events.EventsConfigurationTestProvider;
import org.graylog.events.processor.EventDefinition;
import org.graylog.events.search.MoreSearch;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.engine.QueryEngine;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotResult;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class PivotAggregationSearchTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private SearchJobService searchJobService;
    @Mock
    private QueryEngine queryEngine;
    @Mock
    private EventDefinition eventDefinition;
    @Mock
    private MoreSearch moreSearch;

    @Test
    public void testExtractValuesWithGroupBy() throws Exception {
        final RelativeRange timerange = RelativeRange.create(3600);
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesCard = AggregationSeries.create("abc123", AggregationFunction.CARD, "source");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCard))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch);

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(AbsoluteRange.create(timerange.getFrom(), timerange.getTo()))
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of("a", "b"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of("a"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 84, true, "row-inner"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-inner"))
                        .source("non-leaf")
                        .build())
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of("a", "c"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(2);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of("a", "b"))
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "b"))
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "b"))
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());

        assertThat(results.get(1)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of("a", "c"))
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "c"))
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of("a", "c"))
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());
    }

    @Test
    public void testExtractValuesWithoutGroupBy() throws Exception {
        final RelativeRange timerange = RelativeRange.create(3600);
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesCountNoField = AggregationSeries.create("abc123", AggregationFunction.COUNT, "");
        final AggregationSeries seriesCard = AggregationSeries.create("abc123", AggregationFunction.CARD, "source");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesCountNoField, seriesCard))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch);

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(AbsoluteRange.create(timerange.getFrom(), timerange.getTo()))
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of())
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/<no-field>/abc123"), 23, true, "row-leaf"))
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/card/source/abc123"), 1, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(1);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of())
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(23.0)
                                .series(seriesCountNoField)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(1.0)
                                .series(seriesCard)
                                .build()
                ))
                .build());
    }

    @Test
    public void testExtractValuesWithNullValues() throws Exception {
        final RelativeRange timerange = RelativeRange.create(3600);
        final AggregationSeries seriesCount = AggregationSeries.create("abc123", AggregationFunction.COUNT, "source");
        final AggregationSeries seriesAvg = AggregationSeries.create("abc123", AggregationFunction.AVG, "some_field");
        final AggregationEventProcessorConfig config = AggregationEventProcessorConfig.builder()
                .query("")
                .streams(Collections.emptySet())
                .groupBy(Collections.emptyList())
                .series(ImmutableList.of(seriesCount, seriesAvg))
                .conditions(null)
                .searchWithinMs(30000)
                .executeEveryMs(30000)
                .build();
        final AggregationEventProcessorParameters parameters = AggregationEventProcessorParameters.builder()
                .streams(Collections.emptySet())
                .timerange(timerange)
                .batchSize(500)
                .build();

        final PivotAggregationSearch pivotAggregationSearch = new PivotAggregationSearch(
                config,
                parameters,
                "test",
                eventDefinition,
                searchJobService,
                queryEngine,
                EventsConfigurationTestProvider.create(),
                moreSearch);

        final PivotResult pivotResult = PivotResult.builder()
                .id("test")
                .effectiveTimerange(AbsoluteRange.create(timerange.getFrom(), timerange.getTo()))
                .total(1)
                .addRow(PivotResult.Row.builder()
                        .key(ImmutableList.of())
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/count/source/abc123"), 42, true, "row-leaf"))
                        // A "null" value can happen with some Elasticsearch aggregations (e.g. avg on a non-existent field)
                        .addValue(PivotResult.Value.create(ImmutableList.of("metric/avg/some_field/abc123"), null, true, "row-leaf"))
                        .source("leaf")
                        .build())
                .build();

        final ImmutableList<AggregationKeyResult> results = pivotAggregationSearch.extractValues(pivotResult);

        assertThat(results.size()).isEqualTo(1);

        assertThat(results.get(0)).isEqualTo(AggregationKeyResult.builder()
                .key(ImmutableList.of())
                .seriesValues(ImmutableList.of(
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(42.0)
                                .series(seriesCount)
                                .build(),
                        AggregationSeriesValue.builder()
                                .key(ImmutableList.of())
                                .value(Double.NaN) // For "null" we expect NaN
                                .series(seriesAvg)
                                .build()
                ))
                .build());
    }
}
