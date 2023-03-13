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

import com.google.common.collect.ImmutableSet;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.views.audit.ViewsAuditEventTypes;
import org.graylog.plugins.views.favorites.FavoritesResource;
import org.graylog.plugins.views.migrations.V20181220133700_AddViewsAdminRole;
import org.graylog.plugins.views.migrations.V20190127111728_MigrateWidgetFormatSettings;
import org.graylog.plugins.views.migrations.V20190304102700_MigrateMessageListStructure;
import org.graylog.plugins.views.migrations.V20190805115800_RemoveDashboardStateFromViews;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.V20191125144500_MigrateDashboardsToViews;
import org.graylog.plugins.views.migrations.V20191203120602_MigrateSavedSearchesToViewsSupport.V20191203120602_MigrateSavedSearchesToViews;
import org.graylog.plugins.views.migrations.V20191204000000_RemoveLegacyViewsPermissions;
import org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards.V20200204122000_MigrateUntypedViewsToDashboards;
import org.graylog.plugins.views.migrations.V20200409083200_RemoveRootQueriesFromMigratedDashboards;
import org.graylog.plugins.views.migrations.V20200730000000_AddGl2MessageIdFieldAliasForEvents;
import org.graylog.plugins.views.providers.ExportBackendProvider;
import org.graylog.plugins.views.providers.QuerySuggestionsProvider;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.ValueParameter;
import org.graylog.plugins.views.search.db.InMemorySearchJobService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.db.SearchesCleanUpJob;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.engine.EngineBindings;
import org.graylog.plugins.views.search.engine.QuerySuggestionsService;
import org.graylog.plugins.views.search.engine.SearchConfig;
import org.graylog.plugins.views.search.engine.SearchConfigProvider;
import org.graylog.plugins.views.search.export.ChunkDecorator;
import org.graylog.plugins.views.search.export.DecoratingMessagesExporter;
import org.graylog.plugins.views.search.export.ExportBackend;
import org.graylog.plugins.views.search.export.LegacyChunkDecorator;
import org.graylog.plugins.views.search.export.MessagesExporter;
import org.graylog.plugins.views.search.export.SimpleMessageChunkCsvWriter;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.DashboardsResource;
import org.graylog.plugins.views.search.rest.ExportJobsResource;
import org.graylog.plugins.views.search.rest.FieldTypesResource;
import org.graylog.plugins.views.search.rest.MessageExportFormatFilter;
import org.graylog.plugins.views.search.rest.MessagesResource;
import org.graylog.plugins.views.search.rest.PivotSeriesFunctionsResource;
import org.graylog.plugins.views.search.rest.QualifyingViewsResource;
import org.graylog.plugins.views.search.rest.QueryValidationResource;
import org.graylog.plugins.views.search.rest.SavedSearchesResource;
import org.graylog.plugins.views.search.rest.SearchMetadataResource;
import org.graylog.plugins.views.search.rest.SearchResource;
import org.graylog.plugins.views.search.rest.SuggestionsResource;
import org.graylog.plugins.views.search.rest.ViewsResource;
import org.graylog.plugins.views.search.rest.ViewsRestPermissions;
import org.graylog.plugins.views.search.rest.contexts.SearchUserBinder;
import org.graylog.plugins.views.search.rest.exceptionmappers.IllegalTimeRangeExceptionMapper;
import org.graylog.plugins.views.search.rest.exceptionmappers.MissingCapabilitiesExceptionMapper;
import org.graylog.plugins.views.search.rest.exceptionmappers.PermissionExceptionMapper;
import org.graylog.plugins.views.search.searchtypes.MessageList;
import org.graylog.plugins.views.search.searchtypes.events.EventList;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSort;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSort;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.AutoInterval;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.Time;
import org.graylog.plugins.views.search.searchtypes.pivot.buckets.TimeUnitInterval;
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
import org.graylog.plugins.views.search.validation.FieldTypeValidation;
import org.graylog.plugins.views.search.validation.FieldTypeValidationImpl;
import org.graylog.plugins.views.search.validation.QueryValidationService;
import org.graylog.plugins.views.search.validation.QueryValidationServiceImpl;
import org.graylog.plugins.views.search.validation.validators.FieldValueTypeValidator;
import org.graylog.plugins.views.search.validation.validators.InvalidOperatorsValidator;
import org.graylog.plugins.views.search.validation.validators.UnknownFieldsValidator;
import org.graylog.plugins.views.search.views.RequiresParameterSupport;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AreaVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.BarVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.DataTableVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.HeatmapVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.LineVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.NumberVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ScatterVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeUnitIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ValueConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.WorldMapVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.PivotSortConfig;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SeriesSortConfig;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;
import org.graylog.plugins.views.startpage.StartPageResource;
import org.graylog.plugins.views.startpage.recentActivities.RecentActivityUpdatesListener;
import org.graylog2.contentpacks.facades.DashboardEntityCreator;
import org.graylog2.contentpacks.facades.DashboardFacade;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesServiceImpl;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.resources.suggestions.EntitySuggestionResource;

import java.util.Set;

public class ViewsBindings extends ViewsModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return ImmutableSet.of(new ViewsConfig());
    }

    @Override
    protected void configure() {
        registerExportBackendProvider();

        addSystemRestResource(DashboardsResource.class);
        addSystemRestResource(StartPageResource.class);
        addSystemRestResource(FavoritesResource.class);
        addSystemRestResource(FieldTypesResource.class);
        addSystemRestResource(MessagesResource.class);
        addSystemRestResource(ExportJobsResource.class);
        addSystemRestResource(PivotSeriesFunctionsResource.class);
        addSystemRestResource(QualifyingViewsResource.class);
        addSystemRestResource(SavedSearchesResource.class);
        addSystemRestResource(SearchResource.class);
        addSystemRestResource(SearchMetadataResource.class);
        addSystemRestResource(ViewsResource.class);
        addSystemRestResource(SuggestionsResource.class);
        addSystemRestResource(QueryValidationResource.class);
        addSystemRestResource(EntitySuggestionResource.class);

        addPermissions(ViewsRestPermissions.class);

        // Calling this once to set up binder, so injection does not fail.
        esQueryDecoratorBinder();

        // filter
        registerJacksonSubtype(AndFilter.class);
        registerJacksonSubtype(OrFilter.class);
        registerJacksonSubtype(StreamFilter.class);
        registerJacksonSubtype(QueryStringFilter.class);

        // query backends for jackson
        registerJacksonSubtype(ElasticsearchQueryString.class);

        // search types
        registerJacksonSubtype(MessageList.class);
        registerJacksonSubtype(Pivot.class);
        registerJacksonSubtype(EventList.class);

        // pivot specs
        registerJacksonSubtype(Values.class);
        registerJacksonSubtype(Time.class);
        registerPivotAggregationFunction(Average.NAME, "Average", Average.class);
        registerPivotAggregationFunction(Cardinality.NAME, "Cardinality", Cardinality.class);
        registerPivotAggregationFunction(Count.NAME, "Count", Count.class);
        registerPivotAggregationFunction(Max.NAME, "Maximum", Max.class);
        registerPivotAggregationFunction(Min.NAME, "Minimum", Min.class);
        registerPivotAggregationFunction(StdDev.NAME, "Standard Deviation", StdDev.class);
        registerPivotAggregationFunction(Sum.NAME, "Sum", Sum.class);
        registerPivotAggregationFunction(SumOfSquares.NAME, "Sum of Squares", SumOfSquares.class);
        registerPivotAggregationFunction(Variance.NAME, "Variance", Variance.class);
        registerPivotAggregationFunction(Percentile.NAME, "Percentile", Percentile.class);
        registerPivotAggregationFunction(Latest.NAME, "Latest Value", Latest.class);

        registerJacksonSubtype(TimeUnitInterval.class);
        registerJacksonSubtype(TimeUnitIntervalDTO.class);
        registerJacksonSubtype(AutoInterval.class);
        registerJacksonSubtype(AutoIntervalDTO.class);

        bind(RecentActivityUpdatesListener.class).asEagerSingleton();

        bind(SearchJobService.class).to(InMemorySearchJobService.class).in(Scopes.SINGLETON);
        bind(MappedFieldTypesService.class).to(MappedFieldTypesServiceImpl.class).in(Scopes.SINGLETON);
        bind(FieldTypeValidation.class).to(FieldTypeValidationImpl.class).in(Scopes.SINGLETON);

        // The order of injections is significant!
        registerQueryValidator(FieldValueTypeValidator.class);
        registerQueryValidator(UnknownFieldsValidator.class);
        registerQueryValidator(InvalidOperatorsValidator.class);

        bind(QueryValidationService.class).to(QueryValidationServiceImpl.class).in(Scopes.SINGLETON);
        bind(ChunkDecorator.class).to(LegacyChunkDecorator.class);
        bind(MessagesExporter.class).to(DecoratingMessagesExporter.class);
        bind(DashboardEntityCreator.class).to(DashboardFacade.class);

        registerWidgetConfigSubtypes();

        registerVisualizationConfigSubtypes();

        addPeriodical(SearchesCleanUpJob.class);

        addMigration(V20181220133700_AddViewsAdminRole.class);
        addMigration(V20190304102700_MigrateMessageListStructure.class);
        addMigration(V20190805115800_RemoveDashboardStateFromViews.class);
        addMigration(V20191204000000_RemoveLegacyViewsPermissions.class);
        addMigration(V20191125144500_MigrateDashboardsToViews.class);
        addMigration(V20191203120602_MigrateSavedSearchesToViews.class);
        addMigration(V20190127111728_MigrateWidgetFormatSettings.class);
        addMigration(V20200204122000_MigrateUntypedViewsToDashboards.class);
        addMigration(V20200409083200_RemoveRootQueriesFromMigratedDashboards.class);
        addMigration(V20200730000000_AddGl2MessageIdFieldAliasForEvents.class);

        addAuditEventTypes(ViewsAuditEventTypes.class);

        registerSortConfigSubclasses();
        registerParameterSubtypes();

        install(new FactoryModuleBuilder().build(ViewRequirements.Factory.class));
        install(new FactoryModuleBuilder().build(SearchRequirements.Factory.class));

        registerViewRequirement(RequiresParameterSupport.class);
        registerSearchRequirement(SearchRequiresParameterSupport.class);

        // trigger capability binder once to set it up
        viewsCapabilityBinder();
        queryMetadataDecoratorBinder();

        registerExceptionMappers();

        addExportFormat(() -> MoreMediaTypes.TEXT_CSV_TYPE);

        jerseyAdditionalComponentsBinder().addBinding().toInstance(SimpleMessageChunkCsvWriter.class);
        jerseyAdditionalComponentsBinder().addBinding().toInstance(MessageExportFormatFilter.class);
        jerseyAdditionalComponentsBinder().addBinding().toInstance(SearchUserBinder.class);

        bind(SearchConfig.class).toProvider(SearchConfigProvider.class);

        binder().bind(QuerySuggestionsService.class).toProvider(QuerySuggestionsProvider.class);

        // The ViewResolver binder must be explicitly initialized to avoid an initialization error when
        // no values are bound.
        viewResolverBinder();

        install(new EngineBindings());
    }

    private void registerExportBackendProvider() {
        binder().bind(ExportBackend.class).toProvider(ExportBackendProvider.class);
    }

    private void registerSortConfigSubclasses() {
        registerJacksonSubtype(SeriesSortConfig.class);
        registerJacksonSubtype(PivotSortConfig.class);
        registerJacksonSubtype(SeriesSort.class);
        registerJacksonSubtype(PivotSort.class);
    }

    private void registerWidgetConfigSubtypes() {
        registerJacksonSubtype(AggregationConfigDTO.class);
        registerJacksonSubtype(MessageListConfigDTO.class);

        registerJacksonSubtype(TimeHistogramConfigDTO.class);
        registerJacksonSubtype(ValueConfigDTO.class);
    }

    private void registerVisualizationConfigSubtypes() {
        registerJacksonSubtype(WorldMapVisualizationConfigDTO.class);
        registerJacksonSubtype(BarVisualizationConfigDTO.class);
        registerJacksonSubtype(NumberVisualizationConfigDTO.class);
        registerJacksonSubtype(LineVisualizationConfigDTO.class);
        registerJacksonSubtype(AreaVisualizationConfigDTO.class);
        registerJacksonSubtype(HeatmapVisualizationConfigDTO.class);
        registerJacksonSubtype(DataTableVisualizationConfigDTO.class);
        registerJacksonSubtype(ScatterVisualizationConfigDTO.class);
    }

    private void registerParameterSubtypes() {
        registerJacksonSubtype(ValueParameter.class);
    }

    private void registerExceptionMappers() {
        addJerseyExceptionMapper(MissingCapabilitiesExceptionMapper.class);
        addJerseyExceptionMapper(PermissionExceptionMapper.class);
        addJerseyExceptionMapper(IllegalTimeRangeExceptionMapper.class);
    }
}
