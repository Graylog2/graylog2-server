package org.graylog.plugins.views;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog.plugins.views.audit.EnterpriseAuditEventTypes;
import org.graylog.plugins.views.migrations.V20180817120900_AddViewsUsers;
import org.graylog.plugins.views.migrations.V20181220133700_AddViewsAdminRole;
import org.graylog.plugins.views.migrations.V20190304102700_MigrateMessageListStructure;
import org.graylog.plugins.views.search.SearchRequirements;
import org.graylog.plugins.views.search.SearchRequiresParameterSupport;
import org.graylog.plugins.views.search.db.InMemorySearchJobService;
import org.graylog.plugins.views.search.db.SearchJobService;
import org.graylog.plugins.views.search.db.SearchesCleanUpJob;
import org.graylog.plugins.views.search.elasticsearch.ElasticsearchQueryString;
import org.graylog.plugins.views.search.filter.AndFilter;
import org.graylog.plugins.views.search.filter.OrFilter;
import org.graylog.plugins.views.search.filter.QueryStringFilter;
import org.graylog.plugins.views.search.filter.StreamFilter;
import org.graylog.plugins.views.search.rest.EnterpriseSearchRestPermissions;
import org.graylog.plugins.views.search.rest.FieldTypesResource;
import org.graylog.plugins.views.search.rest.PivotSeriesFunctionsResource;
import org.graylog.plugins.views.search.rest.SearchResource;
import org.graylog.plugins.views.search.views.RequiresParameterSupport;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.rest.ViewSharingResource;
import org.graylog.plugins.views.search.rest.ViewsResource;
import org.graylog.plugins.views.search.searchtypes.MessageList;
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
import org.graylog.plugins.views.search.searchtypes.pivot.series.Max;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Min;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Percentile;
import org.graylog.plugins.views.search.searchtypes.pivot.series.StdDev;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Sum;
import org.graylog.plugins.views.search.searchtypes.pivot.series.SumOfSquares;
import org.graylog.plugins.views.search.searchtypes.pivot.series.Variance;
import org.graylog.plugins.views.search.views.sharing.AllUsersOfInstance;
import org.graylog.plugins.views.search.views.sharing.AllUsersOfInstanceStrategy;
import org.graylog.plugins.views.search.views.sharing.SpecificRoles;
import org.graylog.plugins.views.search.views.sharing.SpecificRolesStrategy;
import org.graylog.plugins.views.search.views.sharing.SpecificUsers;
import org.graylog.plugins.views.search.views.sharing.SpecificUsersStrategy;
import org.graylog.plugins.views.search.views.widgets.aggregation.AggregationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.AutoIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.BarVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeHistogramConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.TimeUnitIntervalDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.ValueConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.WorldMapVisualizationConfigDTO;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.PivotSortConfig;
import org.graylog.plugins.views.search.views.widgets.aggregation.sort.SeriesSortConfig;
import org.graylog.plugins.views.search.views.widgets.messagelist.MessageListConfigDTO;import org.graylog2.plugin.PluginConfigBean;

import java.util.Set;

public class ViewsBindings extends ViewsModule {
    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return ImmutableSet.of(new ViewsConfig());
    }

    @Override
    protected void configure() {
        addRestResource(SearchResource.class);
        addRestResource(ViewsResource.class);
        addRestResource(FieldTypesResource.class);
        addRestResource(PivotSeriesFunctionsResource.class);
        addRestResource(ViewSharingResource.class);

        addPermissions(EnterpriseSearchRestPermissions.class);

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

        // pivot specs
        registerJacksonSubtype(Values.class);
        registerJacksonSubtype(Time.class);
        registerPivotAggregationFunction(Average.NAME, Average.class);
        registerPivotAggregationFunction(Cardinality.NAME, Cardinality.class);
        registerPivotAggregationFunction(Count.NAME, Count.class);
        registerPivotAggregationFunction(Max.NAME, Max.class);
        registerPivotAggregationFunction(Min.NAME, Min.class);
        registerPivotAggregationFunction(StdDev.NAME, StdDev.class);
        registerPivotAggregationFunction(Sum.NAME, Sum.class);
        registerPivotAggregationFunction(SumOfSquares.NAME, SumOfSquares.class);
        registerPivotAggregationFunction(Variance.NAME, Variance.class);
        registerPivotAggregationFunction(Percentile.NAME, Percentile.class);

        registerJacksonSubtype(TimeUnitInterval.class);
        registerJacksonSubtype(TimeUnitIntervalDTO.class);
        registerJacksonSubtype(AutoInterval.class);
        registerJacksonSubtype(AutoIntervalDTO.class);

        bind(SearchJobService.class).to(InMemorySearchJobService.class).in(Scopes.SINGLETON);

        registerWidgetConfigSubtypes();

        registerVisualizationConfigSubtypes();

        addPeriodical(SearchesCleanUpJob.class);

        addMigration(V20180817120900_AddViewsUsers.class);
        addMigration(V20181220133700_AddViewsAdminRole.class);
        addMigration(V20190304102700_MigrateMessageListStructure.class);

        addAuditEventTypes(EnterpriseAuditEventTypes.class);

        registerViewSharingSubtypes();
        registerSharingStrategies();
        registerSortConfigSubclasses();

        install(new FactoryModuleBuilder().build(ViewRequirements.Factory.class));
        install(new FactoryModuleBuilder().build(SearchRequirements.Factory.class));

        registerViewRequirement(RequiresParameterSupport.class);
        registerSearchRequirement(SearchRequiresParameterSupport.class);

        // trigger capability binder once to set it up
        viewsCapabilityBinder();
        queryMetadataDecoratorBinder();
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
    }

    private void registerViewSharingSubtypes() {
        registerJacksonSubtype(AllUsersOfInstance.class);
        registerJacksonSubtype(SpecificRoles.class);
        registerJacksonSubtype(SpecificUsers.class);
    }

    private void registerSharingStrategies() {
        registerSharingStrategy(AllUsersOfInstance.TYPE, AllUsersOfInstanceStrategy.class);
        registerSharingStrategy(SpecificRoles.TYPE, SpecificRolesStrategy.class);
        registerSharingStrategy(SpecificUsers.TYPE, SpecificUsersStrategy.class);
    }}
