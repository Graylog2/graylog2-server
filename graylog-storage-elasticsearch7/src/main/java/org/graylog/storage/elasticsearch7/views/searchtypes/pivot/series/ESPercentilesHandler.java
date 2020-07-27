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
package org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Percentiles;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.PercentilesAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESPercentilesHandler extends ESPivotSeriesSpecHandler<Percentile, Percentiles> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Percentile percentileSpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final PercentilesAggregationBuilder percentiles = AggregationBuilders.percentiles(name).field(percentileSpec.field()).percentiles(percentileSpec.percentile());
        record(queryContext, pivot, percentileSpec, name, Percentiles.class);
        return Optional.of(percentiles);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Percentile pivotSpec,
                                        SearchResponse searchResult,
                                        Percentiles percentilesAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext queryContext) {
        Double percentile = percentilesAggregation.percentile(pivotSpec.percentile());
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Percentile.NAME, percentile));
    }
}
