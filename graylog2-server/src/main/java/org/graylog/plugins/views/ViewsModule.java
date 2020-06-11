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
package org.graylog.plugins.views;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.plugins.views.search.QueryMetadataDecorator;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.rest.SeriesDescription;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.sharing.SharingStrategy;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;

public abstract class ViewsModule extends PluginModule {
    protected void registerQueryMetadataDecorator(Class<? extends QueryMetadataDecorator> queryMetadataDecorator) {
        queryMetadataDecoratorBinder().addBinding().to(queryMetadataDecorator);
    }

    protected Multibinder<QueryMetadataDecorator> queryMetadataDecoratorBinder() {
        return Multibinder.newSetBinder(binder(), QueryMetadataDecorator.class);
    }

    protected void registerProvidedViewsCapability(String capability, PluginMetaData plugin) {
        viewsCapabilityBinder().addBinding(capability).toInstance(plugin);
    }

    protected MapBinder<String, PluginMetaData> viewsCapabilityBinder() {
        return MapBinder.newMapBinder(binder(), String.class, PluginMetaData.class);
    }

    protected void registerViewRequirement(Class<? extends Requirement<ViewDTO>> viewRequirement) {
        viewRequirementBinder().addBinding().to(viewRequirement);
    }

    protected Multibinder<Requirement<ViewDTO>> viewRequirementBinder() {
        return Multibinder.newSetBinder(binder(), new TypeLiteral<Requirement<ViewDTO>>() {});
    }

    protected void registerSearchRequirement(Class<? extends Requirement<Search>> searchRequirement) {
        searchRequirementBinder().addBinding().to(searchRequirement);
    }

    protected Multibinder<Requirement<Search>> searchRequirementBinder() {
        return Multibinder.newSetBinder(binder(), new TypeLiteral<Requirement<Search>>() {});
    }

    protected MapBinder<String, SeriesDescription> seriesSpecBinder() {
        return MapBinder.newMapBinder(binder(), String.class, SeriesDescription.class);
    }

    protected void registerPivotAggregationFunction(String name, Class<? extends SeriesSpec> seriesSpecClass) {
        registerJacksonSubtype(seriesSpecClass);
        seriesSpecBinder().addBinding(name).toInstance(SeriesDescription.create(name));
    }

    protected MapBinder<String, SharingStrategy> sharingStrategyBinder() {
        return MapBinder.newMapBinder(binder(), String.class, SharingStrategy.class);
    }

    protected ScopedBindingBuilder registerSharingStrategy(String type, Class<? extends SharingStrategy> sharingStrategy) {
        return sharingStrategyBinder().addBinding(type).to(sharingStrategy);
    }

    protected MapBinder<String, QueryBackend<? extends GeneratedQueryContext>> queryBackendBinder() {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {});

    }

    protected ScopedBindingBuilder registerQueryBackend(String name, Class<? extends QueryBackend<? extends GeneratedQueryContext>> implementation) {
        return queryBackendBinder().addBinding(name).to(implementation);
    }

    protected void registerESQueryDecorator(Class<? extends QueryStringDecorator> esQueryDecorator) {
        esQueryDecoratorBinder().addBinding().to(esQueryDecorator);
    }

    protected Multibinder<QueryStringDecorator> esQueryDecoratorBinder() {
        return Multibinder.newSetBinder(binder(), QueryStringDecorator.class);
    }
}
