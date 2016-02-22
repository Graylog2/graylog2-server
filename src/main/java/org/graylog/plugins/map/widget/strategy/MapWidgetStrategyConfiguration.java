package org.graylog.plugins.map.widget.strategy;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
public abstract class MapWidgetStrategyConfiguration {

    public abstract String query();

    @Nullable
    public abstract String streamId();

    public abstract String field();

    public abstract TimeRange timeRange();

    public static MapWidgetStrategyConfiguration create(String query,
                                                        String streamId,
                                                        String field,
                                                        TimeRange timeRange) {
        return new AutoValue_MapWidgetStrategyConfiguration(query, streamId, field, timeRange);
    }

    public static MapWidgetStrategyConfiguration create(Map<String, Object> config,
                                                        TimeRange timeRange) {

        final String query = (String) config.get("query");
        final String streamId = (config.containsKey("stream_id") ? (String) config.get("stream_id") : null);
        final String field = (String) config.get("field");

        return MapWidgetStrategyConfiguration.create(query, streamId, field, timeRange);
    }
}
