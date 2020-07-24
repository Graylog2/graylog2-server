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
package org.graylog.storage.elasticsearch7;

import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.views.ViewsModule;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
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
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivot;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESDateRangeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESTimeHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.buckets.ESValuesHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESAverageHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESCardinalityHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESCountHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESMaxHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESMinHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESPercentilesHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESStdDevHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESSumHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESSumOfSquaresHandler;
import org.graylog.storage.elasticsearch7.views.searchtypes.pivot.series.ESVarianceHandler;

import static org.graylog.storage.elasticsearch7.Elasticsearch7Plugin.SUPPORTED_ES_VERSION;

public class ViewsESBackendModule extends ViewsModule {
    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(ESGeneratedQueryContext.Factory.class));

        registerQueryBackend();

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

        registerPivotBucketHandler(Values.NAME, ESValuesHandler.class);
        registerPivotBucketHandler(Time.NAME, ESTimeHandler.class);
        registerPivotBucketHandler(DateRangeBucket.NAME, ESDateRangeHandler.class);

        bindExportBackend().to(ElasticsearchExportBackend.class);
        bindRequestStrategy().to(org.graylog.storage.elasticsearch7.views.export.Scroll.class);
    }

    private LinkedBindingBuilder<RequestStrategy> bindRequestStrategy() {
        return bind(RequestStrategy.class);
    }

    private LinkedBindingBuilder<ExportBackend> bindExportBackend() {
        return bindExportBackend(SUPPORTED_ES_VERSION);
    }

    private void registerQueryBackend() {
        registerQueryBackend(SUPPORTED_ES_VERSION, ElasticsearchQueryString.NAME, ElasticsearchBackend.class);
    }

    private MapBinder<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> pivotBucketHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>>() {});

    }

    private ScopedBindingBuilder registerPivotBucketHandler(
            String name,
            Class<? extends ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> implementation
    ) {
        return pivotBucketHandlerBinder().addBinding(name).to(implementation);
    }

    protected MapBinder<String, ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> pivotSeriesHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>>() {});

    }

    private ScopedBindingBuilder registerPivotSeriesHandler(
            String name,
            Class<? extends ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> implementation
    ) {
        return pivotSeriesHandlerBinder().addBinding(name).to(implementation);
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
