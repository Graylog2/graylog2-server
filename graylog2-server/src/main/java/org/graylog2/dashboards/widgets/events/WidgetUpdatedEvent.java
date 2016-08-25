package org.graylog2.dashboards.widgets.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.dashboards.widgets.DashboardWidget;

@AutoValue
@JsonAutoDetect
public abstract class WidgetUpdatedEvent {
    private static final String FIELD_WIDGET_ID = "widget_id";

    @JsonProperty(FIELD_WIDGET_ID)
    public abstract String widgetId();

    @JsonCreator
    public static WidgetUpdatedEvent create(@JsonProperty(FIELD_WIDGET_ID) String widgetId) {
        return new AutoValue_WidgetUpdatedEvent(widgetId);
    }

    public static WidgetUpdatedEvent create(DashboardWidget widget) {
        return new AutoValue_WidgetUpdatedEvent(widget.getId());
    }
}
