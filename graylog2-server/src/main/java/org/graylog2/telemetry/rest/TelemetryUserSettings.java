package org.graylog2.telemetry.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import javax.validation.constraints.NotNull;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class TelemetryUserSettings {

    @JsonCreator
    public static TelemetryUserSettings create(@JsonProperty("telemetry_enabled") @NotNull Boolean isTelemetryEnabled,
                                               @JsonProperty("telemetry_permission_asked") @NotNull Boolean isTelemetryPermissionAsked) {
        return new AutoValue_TelemetryUserSettings(isTelemetryEnabled, isTelemetryPermissionAsked);
    }

    @JsonProperty
    public abstract Boolean isTelemetryEnabled();

    @JsonProperty
    public abstract Boolean isTelemetryPermissionAsked();
}
