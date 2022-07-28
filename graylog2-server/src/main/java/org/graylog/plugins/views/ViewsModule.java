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
package org.graylog.plugins.views;

import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.OptionalBinder;
import org.graylog.plugins.views.search.QueryMetadataDecorator;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog.plugins.views.search.engine.QueryStringDecorator;
import org.graylog.plugins.views.search.engine.normalization.PostValidation;
import org.graylog.plugins.views.search.engine.normalization.SearchNormalizer;
import org.graylog.plugins.views.search.engine.validation.SearchValidator;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.rest.SeriesDescription;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.graylog.plugins.views.search.validation.QueryValidator;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.VersionAwareModule;
import org.graylog2.storage.SearchVersion;

public abstract class ViewsModule extends VersionAwareModule {
    protected LinkedBindingBuilder<ExportBackend> bindExportBackend(SearchVersion supportedVersion) {
        return bindForVersion(supportedVersion, ExportBackend.class);
    }

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

    protected void registerPivotAggregationFunction(String name, String description, Class<? extends SeriesSpec> seriesSpecClass) {
        registerJacksonSubtype(seriesSpecClass);
        seriesSpecBinder().addBinding(name).toInstance(SeriesDescription.create(name, description));
    }

    protected MapBinder<String, QueryBackend<? extends GeneratedQueryContext>> queryBackendBinder(SearchVersion version) {
        return MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {});

    }

    protected ScopedBindingBuilder registerQueryBackend(SearchVersion version, String name, Class<? extends QueryBackend<? extends GeneratedQueryContext>> implementation) {
        return queryBackendBinder(version).addBinding(name).to(implementation);
    }

    protected void registerESQueryDecorator(Class<? extends QueryStringDecorator> esQueryDecorator) {
        esQueryDecoratorBinder().setBinding().to(esQueryDecorator);
    }

    protected OptionalBinder<QueryStringDecorator> esQueryDecoratorBinder() {
        return OptionalBinder.newOptionalBinder(binder(), QueryStringDecorator.class);
    }

    protected void registerQueryValidator(Class<? extends QueryValidator> validator) {
        queryValidatorMultibinder().addBinding().to(validator);
    }

    protected Multibinder<QueryValidator> queryValidatorMultibinder() {
        return Multibinder.newSetBinder(binder(), QueryValidator.class);
    }

    protected void registerSearchNormalizer(Class<? extends SearchNormalizer> normalizer) {
        if (normalizer.getAnnotation(PostValidation.class) != null) {
            searchPostValidationNormalizerBinder().addBinding().to(normalizer);
        } else {
            searchNormalizerBinder().addBinding().to(normalizer);
        }
    }

    protected Multibinder<SearchNormalizer> searchNormalizerBinder() {
        return Multibinder.newSetBinder(binder(), SearchNormalizer.class);
    }

    protected Multibinder<SearchNormalizer> searchPostValidationNormalizerBinder() {
        return Multibinder.newSetBinder(binder(), SearchNormalizer.class, PostValidation.class);
    }

    protected void registerSearchValidator(Class<? extends SearchValidator> validator) {
        searchValidatorBinder().addBinding().to(validator);
    }

    protected Multibinder<SearchValidator> searchValidatorBinder() {
        return Multibinder.newSetBinder(binder(), SearchValidator.class);
    }
}
