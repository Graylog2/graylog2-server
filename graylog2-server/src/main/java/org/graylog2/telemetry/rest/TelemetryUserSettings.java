package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TelemetryUserSettings {

    public static Builder builder() {
        return new AutoValue_TelemetryUserSettings.Builder();
    }

    @JsonCreator
    public static TelemetryUserSettings create(@JsonProperty("telemetry_enabled") Boolean enabled,
                                               @JsonProperty("telemetry_permission_asked") Boolean permissionAsked) {
        return builder()
                .telemetryEnabled(enabled)
                .telemetryPermissionAsked(permissionAsked)
                .build();
    }

    @JsonProperty
    public abstract Boolean telemetryEnabled();

    @JsonProperty
    public abstract Boolean telemetryPermissionAsked();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder telemetryEnabled(Boolean enabled);


        public abstract Builder telemetryPermissionAsked(Boolean permissionAsked);

        public abstract TelemetryUserSettings build();
    }
}
