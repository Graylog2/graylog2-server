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
package org.graylog.events.procedures;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;

/**
 * Executes an existing notification with the event.
 */
public class ExecuteNotification extends Action {
    public static final String NAME = "execute_notification";
    public static final String FIELD_NOTIFICATION_ID = "notification_id";

    @Inject
    public ExecuteNotification(@Assisted ActionDto dto) {
        super(dto);
    }

    public interface Factory extends Action.Factory<ExecuteNotification> {
        @Override
        ExecuteNotification create(ActionDto dto);
    }

    @AutoValue
    @JsonAutoDetect
    @JsonTypeName(NAME)
    @JsonDeserialize(builder = AutoValue_ExecuteNotification_Config.Builder.class)
    public static abstract class Config implements ActionConfig {
        @Override
        @JsonProperty(TYPE_FIELD)
        public abstract String type();

        @JsonProperty(FIELD_NOTIFICATION_ID)
        public abstract String notificationId();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            @JsonProperty(TYPE_FIELD)
            public abstract Builder type(String type);

            @JsonProperty(FIELD_NOTIFICATION_ID)
            public abstract Builder notificationId(String notificationId);

            @JsonCreator
            public static Builder create() {
                return new AutoValue_ExecuteNotification_Config.Builder().type(NAME);
            }

            public abstract Config build();
        }
    }
}
