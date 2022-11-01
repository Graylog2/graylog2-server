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
package org.graylog.storage.elasticsearch7;

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
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregation;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.ElasticsearchBackend;
import org.graylog.storage.elasticsearch7.views.export.ElasticsearchExportBackend;
import org.graylog.storage.elasticsearch7.views.export.RequestStrategy;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESEventList;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESMessageList;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESDateRangeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESTimeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESValuesHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESAverageHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESCardinalityHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESCountHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESLatestHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESMaxHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESMinHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESPercentilesHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESStdDevHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESSumHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESSumOfSquaresHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESVarianceHandler;
import org.graylog2.storage.SearchVersion;

public class ViewsESBackendModule extends ViewsModule {
    private final SearchVersion supportedSearchVersion;

    public ViewsESBackendModule(SearchVersion supportedSearchVersion) {
        this.supportedSearchVersion = supportedSearchVersion;
    }

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(ESGeneratedQueryContext.Factory.class));

        bindForVersion(supportedSearchVersion, new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {})
                .to(ElasticsearchBackend.class);

        registerESSearchTypeHandler(MessageList.NAME, ESMessageList.class);
        registerESSearchTypeHandler(EventList.NAME, ESEventList.class);
        registerESSearchTypeHandler(Pivot.NAME, ESPivot.class).in(Scopes.SINGLETON);

        registerPivotSeriesHandler(Average.NAME, ESAverageHandler.class);
        registerPivotSeriesHandler(Cardinality.NAME, ESCardinalityHandler.class);
        registerPivotSeriesHandler(Count.NAME, ESCountHandler.class);
        registerPivotSeriesHandler(Max.NAME, ESMaxHandler.class);
        registerPivotSeriesHandler(Min.NAME, ESMinHandler.class);
        registerPivotSeriesHandler(StdDev.NAME, ESStdDevHandler.class);
        registerPivotSeriesHandler(Sum.NAME, ESSumHandler.class);
        registerPivotSeriesHandler(SumOfSquares.NAME, ESSumOfSquaresHandler.class);
        registerPivotSeriesHandler(Variance.NAME, ESVarianceHandler.class);
        registerPivotSeriesHandler(Percentile.NAME, ESPercentilesHandler.class);
        registerPivotSeriesHandler(Latest.NAME, ESLatestHandler.class);

        registerPivotBucketHandler(Values.NAME, ESValuesHandler.class);
        registerPivotBucketHandler(Time.NAME, ESTimeHandler.class);
        registerPivotBucketHandler(DateRangeBucket.NAME, ESDateRangeHandler.class);

        bindExportBackend().to(ElasticsearchExportBackend.class);
        bindRequestStrategy().to(org.graylog.storage.elasticsearch7.views.export.SearchAfter.class);
    }

    private LinkedBindingBuilder<RequestStrategy> bindRequestStrategy() {
        return bind(RequestStrategy.class);
    }

    private LinkedBindingBuilder<ExportBackend> bindExportBackend() {
        return bindExportBackend(supportedSearchVersion);
    }

    private MapBinder<String, ESPivotBucketSpecHandler<? extends BucketSpec>> pivotBucketHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {});

    }

    private void registerPivotBucketHandler(
            String name,
            Class<? extends ESPivotBucketSpecHandler<? extends BucketSpec>> implementation
    ) {
        pivotBucketHandlerBinder().addBinding(name).to(implementation);
    }

    protected MapBinder<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> pivotSeriesHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<>() {});

    }

    private void registerPivotSeriesHandler(
            String name,
            Class<? extends ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> implementation
    ) {
        pivotSeriesHandlerBinder().addBinding(name).to(implementation);
    }

    private MapBinder<String, ESSearchTypeHandler<? extends SearchType>> esSearchTypeHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ESSearchTypeHandler<? extends SearchType>>() {});
    }

    private ScopedBindingBuilder registerESSearchTypeHandler(String name, Class<? extends ESSearchTypeHandler<? extends SearchType>> implementation) {
        return esSearchTypeHandlerBinder().addBinding(name).to(implementation);
    }
}
