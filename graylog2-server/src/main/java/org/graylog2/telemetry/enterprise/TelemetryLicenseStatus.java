package org.graylog2.telemetry.enterprise;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
public abstract class TelemetryLicenseStatus {

    public static Builder builder() {
        return new AutoValue_TelemetryLicenseStatus.Builder();
    }

    @JsonProperty("violated")
    public abstract boolean violated();

    @JsonProperty("expired")
    public abstract boolean expired();

    @JsonProperty("valid")
    public abstract boolean valid();

    @JsonProperty("subject")
    public abstract String subject();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder violated(boolean violated);

        public abstract Builder expired(boolean expired);

        public abstract Builder valid(boolean valid);

        public abstract Builder subject(String subject);

        public abstract TelemetryLicenseStatus build();
    }

}
