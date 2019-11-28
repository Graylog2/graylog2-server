package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.AggregationConfig;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Pivot;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.Series;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class WorldMapConfig extends WidgetConfigBase implements WidgetConfig {
    private static final String MAP_VISUALIZATION = "map";

    public abstract String field();
    public abstract Optional<String> streamId();

    private Series series() {
        return countSeries();
    }

    private Pivot fieldPivot() {
        return valuesPivotForField(field(), 15);
    }

    @Override
    public Set<ViewWidget> toViewWidgets() {
        final ViewWidget.Builder viewWidgetBuilder = createViewWidget()
                .config(AggregationConfig.builder()
                        .rowPivots(Collections.singletonList(fieldPivot()))
                        .series(Collections.singletonList(series()))
                        .visualization(MAP_VISUALIZATION)
                        .build());
        return Collections.singleton(
                streamId()
                        .map(Collections::singleton)
                        .map(viewWidgetBuilder::streams)
                        .orElse(viewWidgetBuilder)
                        .build()
        );
    }

    @JsonCreator
    static WorldMapConfig create(
            @JsonProperty("field") String field,
            @JsonProperty("stream_id") @Nullable String streamId,
            @JsonProperty("query") String query,
            @JsonProperty("timerange") TimeRange timerange
    ) {
        return new AutoValue_WorldMapConfig(
                query,
                timerange,
                field,
                Optional.ofNullable(streamId)
        );
    }
}
