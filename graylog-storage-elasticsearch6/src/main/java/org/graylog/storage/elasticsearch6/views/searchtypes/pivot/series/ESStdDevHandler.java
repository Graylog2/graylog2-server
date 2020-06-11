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
package org.graylog.storage.elasticsearch6.views.searchtypes.pivot.series;

import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.ExtendedStatsAggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.metrics.stats.extended.ExtendedStatsAggregationBuilder;
import org.graylog.storage.elasticsearch6.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch6.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESStdDevHandler extends ESPivotSeriesSpecHandler<StdDev, ExtendedStatsAggregation> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, StdDev stddevSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final ExtendedStatsAggregationBuilder stddev = AggregationBuilders.extendedStats(name).field(stddevSpec.field());
        record(queryContext, pivot, stddevSpec, name, ExtendedStatsAggregation.class);
        return Optional.of(stddev);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, StdDev pivotSpec,
                                        SearchResult searchResult,
                                        ExtendedStatsAggregation stddevAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), StdDev.NAME, stddevAggregation.getStdDeviation()));
    }
}
