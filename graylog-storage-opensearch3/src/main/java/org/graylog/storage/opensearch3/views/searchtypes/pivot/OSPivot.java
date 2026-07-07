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
package org.graylog.storage.opensearch3.views.searchtypes.pivot;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.inject.Inject;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.storage.opensearch3.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch3.views.searchtypes.OSSearchTypeHandler;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.core.msearch.MultiSearchItem;

import java.util.Map;

/**
 * Handles the {@link Pivot} search type for OpenSearch by delegating query generation to {@link PivotQueryGenerator}
 * and result extraction to {@link PivotResultProcessor}.
 */
public class OSPivot implements OSSearchTypeHandler<Pivot> {
    private final PivotQueryGenerator queryGenerator;
    private final PivotResultProcessor resultProcessor;

    @Inject
    public OSPivot(Map<String, OSPivotBucketSpecHandler<? extends BucketSpec>> bucketHandlers,
                   Map<String, OSPivotSeriesSpecHandler<? extends SeriesSpec>> seriesHandlers,
                   EffectiveTimeRangeExtractor effectiveTimeRangeExtractor) {
        this.queryGenerator = new PivotQueryGenerator(bucketHandlers, seriesHandlers);
        this.resultProcessor = new PivotResultProcessor(bucketHandlers, seriesHandlers, effectiveTimeRangeExtractor);
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        queryGenerator.generate(query, pivot, queryContext);
    }

    @WithSpan
    @Override
    public SearchType.Result doExtractResult(Query query, Pivot pivot, MultiSearchItem<JsonData> queryResult, OSGeneratedQueryContext queryContext) {
        return resultProcessor.extract(query, pivot, queryResult, queryContext);
    }
}
