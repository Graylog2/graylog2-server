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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.CardinalityAggregationBuilder;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.stream.Stream;

public class ESCardinalityHandler extends ESPivotSeriesSpecHandler<Cardinality, org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality> {
    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Cardinality cardinalitySpec, ESPivot searchTypeHandler, ESGeneratedQueryContext queryContext) {
        final CardinalityAggregationBuilder card = AggregationBuilders.cardinality(name).field(cardinalitySpec.field());
        record(queryContext, pivot, cardinalitySpec, name, org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality.class);
        return Optional.of(card);
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot, Cardinality pivotSpec,
                                        SearchResponse searchResult,
                                        org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.metrics.Cardinality cardinalityAggregation,
                                        ESPivot searchTypeHandler,
                                        ESGeneratedQueryContext esGeneratedQueryContext) {
        return Stream.of(ESPivotSeriesSpecHandler.Value.create(pivotSpec.id(), Cardinality.NAME, cardinalityAggregation.getValue()));
    }
}
