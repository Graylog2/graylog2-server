package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.TimeRange;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class WorldMapConfig implements WidgetConfig {
    public abstract String field();
    @Nullable
    public abstract String streamId();

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
                streamId
        );
    }
}
