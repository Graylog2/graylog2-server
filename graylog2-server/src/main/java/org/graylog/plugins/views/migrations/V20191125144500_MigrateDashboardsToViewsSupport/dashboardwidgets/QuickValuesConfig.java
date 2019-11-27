package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SeriesSortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.ValueConfig;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class QuickValuesConfig extends WidgetConfigBase implements WidgetConfig {
    private static final String VISUALIZATION_PIE = "pie";
    private static final String VISUALIZATION_TABLE = "table";

    public abstract String field();
    public abstract Boolean showDataTable();
    public abstract Boolean showPieChart();
    public abstract Integer limit();
    public abstract Integer dataTableLimit();
    public abstract String sortOrder();
    public abstract String stackedFields();

    private Series series() {
        return Series.createFromString("count()").build();
    }

    private Pivot valuesPivotForField(String field, int limit) {
        return Pivot.valuesBuilder()
                .field(field)
                .config(ValueConfig.ofLimit(limit))
                .build();
    }

    private List<Pivot> stackedFieldPivots() {
        return Splitter.on(",")
                .splitToList(stackedFields())
                .stream()
                .map(fieldName -> valuesPivotForField(fieldName, 15))
                .collect(Collectors.toList());
    }

    private Pivot piePivot() {
        return valuesPivotForField(field(), limit());
    }

    private Pivot dataTablePivot() {
        return Pivot.valuesBuilder()
                .field(field())
                .config(ValueConfig.ofLimit(dataTableLimit()))
                .build();
    }

    private SortConfig.Direction order() {
        switch (sortOrder()) {
            case "asc": return SortConfig.Direction.Ascending;
            case "desc": return SortConfig.Direction.Descending;
        }
        throw new RuntimeException("Unable to parse sort order: "  + sortOrder());
    }

    private SortConfig sort() {
        return SeriesSortConfig.create(field(), order());
    }

    @Override
    public Set<ViewWidget> toViewWidgets() {
        final ImmutableSet.Builder<ViewWidget> viewWidgets = ImmutableSet.builder();
        final AggregationConfig.Builder baseConfigBuilder = AggregationConfig.builder()
                .sort(Collections.singletonList(sort()))
                .series(Collections.singletonList(series()));
        if (showPieChart()) {
            final ViewWidget pieChart = createViewWidget()
                    .config(
                            baseConfigBuilder
                                    .rowPivots(ImmutableList.<Pivot>builder().add(piePivot()).addAll(stackedFieldPivots()).build())
                                    .visualization(VISUALIZATION_PIE)
                                    .build()
                    )
                    .build();
            viewWidgets.add(pieChart);
        }
        if (showDataTable()) {
            final ViewWidget pieChart = createViewWidget()
                    .config(
                            baseConfigBuilder
                                    .rowPivots(ImmutableList.<Pivot>builder().add(dataTablePivot()).addAll(stackedFieldPivots()).build())
                                    .visualization(VISUALIZATION_TABLE)
                                    .build()
                    )
                    .build();
            viewWidgets.add(pieChart);
        }

        return viewWidgets.build();
    }

    @JsonCreator
    static QuickValuesConfig create(
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("show_data_table") Boolean showDataTable,
            @JsonProperty("show_pie_chart") Boolean showPieChart,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("data_table_limit") Integer dataTableLimit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields
    ) {
        return new AutoValue_QuickValuesConfig(
                query,
                timerange,
                field,
                showDataTable,
                showPieChart,
                limit,
                dataTableLimit,
                sortOrder,
                stackedFields
        );
    }
}
