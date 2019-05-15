package org.graylog.plugins.enterprise;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import io.searchbox.core.search.aggregation.Aggregation;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.SearchType;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.ESSearchTypeHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotBucketSpecHandler;
import org.graylog.plugins.enterprise.search.elasticsearch.searchtypes.pivot.ESPivotSeriesSpecHandler;
import org.graylog.plugins.enterprise.search.engine.GeneratedQueryContext;
import org.graylog.plugins.enterprise.search.engine.QueryBackend;
import org.graylog.plugins.enterprise.search.rest.SeriesDescription;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.BucketSpec;
import org.graylog.plugins.enterprise.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.enterprise.search.views.sharing.SharingStrategy;
import org.graylog2.plugin.PluginModule;

public abstract class ViewsModule extends PluginModule {
    protected MapBinder<String, SeriesDescription> seriesSpecBinder() {
        return MapBinder.newMapBinder(binder(), String.class, SeriesDescription.class);
    }

    protected void registerPivotAggregationFunction(String name, Class<? extends SeriesSpec> seriesSpecClass) {
        registerJacksonSubtype(seriesSpecClass);
        seriesSpecBinder().addBinding(name).toInstance(SeriesDescription.create(name));
    }

    protected ScopedBindingBuilder registerParameterBinding(Class<? extends Parameter.Binding> bindingClass,
                                                            String bindingName,
                                                            Class<? extends Parameter.BindingHandler> bindingHandler) {
        registerJacksonSubtype(bindingClass, bindingName);
        MapBinder<String, Parameter.BindingHandler> bindingHandlerBinder =
                MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class),
                        new TypeLiteral<Parameter.BindingHandler>() {});

        return bindingHandlerBinder.addBinding(bindingName).to(bindingHandler);
    }

    protected MapBinder<String, SharingStrategy> sharingStrategyBinder() {
        return MapBinder.newMapBinder(binder(), String.class, SharingStrategy.class);
    }

    protected ScopedBindingBuilder registerSharingStrategy(String type, Class<? extends SharingStrategy> sharingStrategy) {
        return sharingStrategyBinder().addBinding(type).to(sharingStrategy);
    }

    protected MapBinder<String, ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>> pivotBucketHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ESPivotBucketSpecHandler<? extends BucketSpec, ? extends Aggregation>>() {});

    }

    protected ScopedBindingBuilder registerPivotBucketHandler(
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

    protected ScopedBindingBuilder registerPivotSeriesHandler(
            String name,
            Class<? extends ESPivotSeriesSpecHandler<? extends SeriesSpec, ? extends Aggregation>> implementation
    ) {
        return pivotSeriesHandlerBinder().addBinding(name).to(implementation);
    }

    protected MapBinder<String, QueryBackend<? extends GeneratedQueryContext>> queryBackendBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {});

    }

    protected ScopedBindingBuilder registerQueryBackend(String name, Class<? extends QueryBackend<? extends GeneratedQueryContext>> implementation) {
        return queryBackendBinder().addBinding(name).to(implementation);
    }

    protected MapBinder<String, ESSearchTypeHandler<? extends SearchType>> esSearchTypeHandlerBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<ESSearchTypeHandler<? extends SearchType>>() {});
    }

    protected ScopedBindingBuilder registerESSearchTypeHandler(String name, Class<? extends ESSearchTypeHandler<? extends SearchType>> implementation) {
        return esSearchTypeHandlerBinder().addBinding(name).to(implementation);
    }
}
