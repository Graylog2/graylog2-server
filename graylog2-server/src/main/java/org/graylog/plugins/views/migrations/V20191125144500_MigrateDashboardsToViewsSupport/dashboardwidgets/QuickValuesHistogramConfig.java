package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AutoInterval;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.BarVisualizationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SeriesSortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.SortConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.TimeHistogramConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@AutoValue
@JsonAutoDetect
public abstract class QuickValuesHistogramConfig extends WidgetConfigBase implements WidgetConfig {
    private static final String BAR_VISUALIZATION = "bar";

    public abstract String field();
    public abstract Integer limit();
    public abstract String sortOrder();
    public abstract String stackedFields();
    public abstract Optional<String> interval();

    private Series series() {
        return countSeries();
    }

    private SortConfig.Direction order() {
        return sortDirection(sortOrder());
    }

    private SortConfig sort() {
        return SeriesSortConfig.create(field(), order());
    }


    private List<Pivot> stackedFieldPivots() {
        return Strings.isNullOrEmpty(stackedFields())
                ? Collections.emptyList()
                : Splitter.on(",")
                .splitToList(stackedFields())
                .stream()
                .map(fieldName -> valuesPivotForField(fieldName, limit()))
                .collect(Collectors.toList());
    }

    @Override
    public Set<ViewWidget> toViewWidgets() {
        return Collections.singleton(
                createViewWidget()
                        .config(
                                AggregationConfig.builder()
                                        .rowPivots(Collections.singletonList(
                                                Pivot.timeBuilder()
                                                        .field(TIMESTAMP_FIELD)
                                                        .config(TimeHistogramConfig.builder()
                                                                .interval(
                                                                        interval()
                                                                                .map(this::timestampInterval)
                                                                                .orElse(AutoInterval.create())
                                                                ).build())
                                                        .build()
                                        ))
                                        .columnPivots(stackedFieldPivots())
                                        .series(Collections.singletonList(series()))
                                        .sort(Collections.singletonList(sort()))
                                        .visualization(BAR_VISUALIZATION)
                                        .visualizationConfig(
                                                BarVisualizationConfig.builder()
                                                        .barmode(BarVisualizationConfig.BarMode.stack)
                                                        .build()
                                        )
                                .build()
                        ).build()
        );
    }

    @JsonCreator
    static QuickValuesHistogramConfig create(
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange,
            @JsonProperty("field") String field,
            @JsonProperty("limit") Integer limit,
            @JsonProperty("sort_order") String sortOrder,
            @JsonProperty("stacked_fields") String stackedFields,
            @JsonProperty("interval") @Nullable String interval,
            @JsonProperty("stream_id") @Nullable String streamId
    ) {
        return new AutoValue_QuickValuesHistogramConfig(
                query,
                timerange,
                Optional.ofNullable(streamId),
                field,
                limit,
                sortOrder,
                stackedFields,
                Optional.ofNullable(interval)
        );
    }
}
