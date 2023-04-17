/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
