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
package org.graylog2.telemetry.user.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
public abstract class TelemetryUserSettingsDto {

    public static final String FIELD_USER_ID = "user_id";

    public static Builder builder() {
        return new AutoValue_TelemetryUserSettingsDto.Builder();
    }

    @JsonCreator
    public static TelemetryUserSettingsDto create(@JsonProperty("id") @Id @ObjectId String id,
                                                  @JsonProperty(FIELD_USER_ID) String userId,
                                                  @JsonProperty("telemetry_enabled") Boolean telemetryEnabled,
                                                  @JsonProperty("telemetry_permission_asked") Boolean telemetryPermissionAsked) {
        return builder()
                .id(id)
                .userId(userId)
                .telemetryEnabled(telemetryEnabled)
                .telemetryPermissionAsked(telemetryPermissionAsked)
                .build();
    }

    @Id
    @ObjectId
    @Nullable
    @JsonProperty
    public abstract String id();

    @JsonProperty
    public abstract String userId();

    @JsonProperty
    public abstract Boolean telemetryEnabled();

    @JsonProperty
    public abstract Boolean telemetryPermissionAsked();

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String id);

        public abstract Builder telemetryEnabled(Boolean isTelemetryEnabled);

        public abstract Builder telemetryPermissionAsked(Boolean isTelemetryPermissionAsked);

        public abstract Builder userId(String userId);

        public abstract TelemetryUserSettingsDto build();
    }
}
