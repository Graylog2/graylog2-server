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
package org.graylog.storage.opensearch2.views.searchtypes.pivot.series;

import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.common.xcontent.XContentBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilder;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.AggregationBuilders;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.HasAggregations;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.bucket.missing.Missing;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ValueCount;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.metrics.ValueCountAggregationBuilder;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class OSCountHandler extends OSPivotSeriesSpecHandler<Count, ValueCount> {
    private static final Logger LOG = LoggerFactory.getLogger(OSCountHandler.class);

    @Nonnull
    @Override
    public Optional<AggregationBuilder> doCreateAggregation(String name, Pivot pivot, Count count, OSSearchTypeHandler<Pivot> searchTypeHandler, OSGeneratedQueryContext queryContext) {
        final String field = count.field();
        if (field == null) {
            // doc_count is always present in elasticsearch's bucket aggregations, no need to add it
            return Optional.empty();
        } else {
            // the request was for a field count, we have to add a value_count sub aggregation
            final ValueCountAggregationBuilder value = AggregationBuilders.count(name).field(field);
            record(queryContext, pivot, count, name, ValueCount.class);
            return Optional.of(value);
        }
    }

    @Override
    public Stream<Value> doHandleResult(Pivot pivot,
                                        Count count,
                                        SearchResponse searchResult,
                                        ValueCount valueCount,
                                        OSSearchTypeHandler<Pivot> searchTypeHandler,
                                        OSGeneratedQueryContext esGeneratedQueryContext) {
        final Object value;
        if (valueCount == null) {
            LOG.error("Unexpected null aggregation result, returning 0 for the count. This is a bug.");
            value = 0;
        } else if (valueCount instanceof MultiBucketsAggregation.Bucket) {
            value = ((MultiBucketsAggregation.Bucket) valueCount).getDocCount();
        } else if (valueCount instanceof Aggregations) {
            value = searchResult.getHits().getTotalHits().value;
        } else {
            value = valueCount.getValue();
        }
        return Stream.of(Value.create(count.id(), Count.NAME, value));
    }

    @Override
    protected Aggregation extractAggregationFromResult(Pivot pivot, PivotSpec spec, HasAggregations aggregations, OSGeneratedQueryContext queryContext) {
        final Tuple2<String, Class<? extends Aggregation>> objects = aggTypes(queryContext, pivot).getTypes(spec);
        if (objects == null) {
            if (aggregations instanceof MultiBucketsAggregation.Bucket) {
                return createValueCount(((MultiBucketsAggregation.Bucket) aggregations).getDocCount());
            } else if (aggregations instanceof Missing) {
                return createValueCount(((Missing) aggregations).getDocCount());
            }
        } else {
            // try to saved sub aggregation type. this might fail if we refer to the total result of the entire result instead of a specific
            // value_count aggregation. we'll handle that special case in doHandleResult above
            return aggregations.getAggregations().get(objects.v1);
        }

        return null;
    }

    private Aggregation createValueCount(final Long docCount) {
        return new ValueCount() {
            @Override
            public long getValue() {
                return docCount;
            }

            @Override
            public double value() {
                return docCount;
            }

            @Override
            public String getValueAsString() {
                return docCount.toString();
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getType() {
                return null;
            }

            @Override
            public Map<String, Object> getMetadata() {
                return null;
            }

            @Override
            public XContentBuilder toXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
                return null;
            }
        };
    }
}
