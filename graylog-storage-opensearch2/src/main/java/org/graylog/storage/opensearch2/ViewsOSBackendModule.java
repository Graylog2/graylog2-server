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
package org.graylog.storage.opensearch2;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.views.ViewsModule;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.DateRangeBucket;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Values;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Average;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Cardinality;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Count;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Latest;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.OpenSearchBackend;
import org.graylog.storage.opensearch2.views.export.OpenSearchExportBackend;
import org.graylog.storage.opensearch2.views.export.RequestStrategy;
import org.graylog.storage.opensearch2.views.searchtypes.OSEventList;
import org.graylog.storage.opensearch2.views.searchtypes.OSMessageList;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotBucketSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivotSeriesSpecHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.OSPivot;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSDateRangeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSTimeHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.buckets.OSValuesHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSAverageHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSCardinalityHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSCountHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSLatestHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSMaxHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSMinHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSPercentilesHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSStdDevHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSSumHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSSumOfSquaresHandler;
import org.graylog.storage.opensearch2.views.searchtypes.pivot.series.OSVarianceHandler;
import org.graylog.storage.opensearch2.views.export.SearchAfter;
import org.graylog2.storage.SearchVersion;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregation;

public class ViewsOSBackendModule extends ViewsModule {
    private final SearchVersion supportedSearchVersion;

    public ViewsOSBackendModule(SearchVersion supportedSearchVersion) {
        this.supportedSearchVersion = supportedSearchVersion;
    }

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(OSGeneratedQueryContext.Factory.class));

        bindForVersion(supportedSearchVersion, new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {})
                .to(OpenSearchBackend.class);

        registerOSSearchTypeHandler(MessageList.NAME, OSMessageList.class);
        registerOSSearchTypeHandler(EventList.NAME, OSEventList.class);
        registerOSSearchTypeHandler(Pivot.NAME, OSPivot.class).in(Scopes.SINGLETON);

        registerPivotSeriesHandler(Average.NAME, OSAverageHandler.class);
        registerPivotSeriesHandler(Cardinality.NAME, OSCardinalityHandler.class);
        registerPivotSeriesHandler(Count.NAME, OSCountHandler.class);
        registerPivotSeriesHandler(Max.NAME, OSMaxHandler.class);
        registerPivotSeriesHandler(Min.NAME, OSMinHandler.class);
        registerPivotSeriesHandler(StdDev.NAME, OSStdDevHandler.class);
        registerPivotSeriesHandler(Sum.NAME, OSSumHandler.class);
        registerPivotSeriesHandler(SumOfSquares.NAME, OSSumOfSquaresHandler.class);
        registerPivotSeriesHandler(Variance.NAME, OSVarianceHandler.class);
        registerPivotSeriesHandler(Percentile.NAME, OSPercentilesHandler.class);
        registerPivotSeriesHandler(Latest.NAME, OSLatestHandler.class);

        registerPivotBucketHandler(Values.NAME, OSValuesHandler.class);
        registerPivotBucketHandler(Time.NAME, OSTimeHandler.class);
        registerPivotBucketHandler(DateRangeBucket.NAME, OSDateRangeHandler.class);

        bindExportBackend().to(OpenSearchExportBackend.class);
        bindRequestStrategy().to(SearchAfter.class);
    }

    private LinkedBindingBuilder<RequestStrategy> bindRequestStrategy() {
        return bind(RequestStrategy.class);
    }

    private LinkedBindingBuilder<ExportBackend> bindExportBackend() {
        return bindExportBackend(supportedSearchVersion);
    }

    private MapBinder<String, OSPivotBucketSpecHandler<? extends BucketSpec>> pivotBucketHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<OSPivotBucketSpecHandler<? extends BucketSpec>>() {});

    }

    private void registerPivotBucketHandler(
            String name,
            Class<? extends OSPivotBucketSpecHandler<? extends BucketSpec>> implementation
    ) {
        pivotBucketHandlerBinder().addBinding(name).to(implementation);
    }

    protected MapBinder<String, OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> pivotSeriesHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {});

    }

    private void registerPivotSeriesHandler(
            String name,
            Class<? extends OSPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> implementation
    ) {
        pivotSeriesHandlerBinder().addBinding(name).to(implementation);
    }

    private MapBinder<String, OSSearchTypeHandler<? extends SearchType>> osSearchTypeHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<OSSearchTypeHandler<? extends SearchType>>() {});
    }

    private ScopedBindingBuilder registerOSSearchTypeHandler(String name, Class<? extends OSSearchTypeHandler<? extends SearchType>> implementation) {
        return osSearchTypeHandlerBinder().addBinding(name).to(implementation);
    }
}
