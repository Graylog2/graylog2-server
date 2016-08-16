package org.graylog2.dashboards.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class DashboardDeletedEvent {
    private static final String FIELD_DASHBOARD_ID = "dashboard_id";

    @JsonProperty(FIELD_DASHBOARD_ID)
    public abstract String dashboardId();

    @JsonCreator
    public static DashboardDeletedEvent create(@JsonProperty(FIELD_DASHBOARD_ID) String dashboardId) {
        return new AutoValue_DashboardDeletedEvent(dashboardId);
    }
}
