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
package org.graylog.events.processor.systemnotification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.processor.EventProcessorParameters;
import org.graylog2.notifications.Notification;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.Map;

@AutoValue
@JsonTypeName(SystemNotificationEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = SystemNotificationEventProcessorParameters.Builder.class)
public abstract class SystemNotificationEventProcessorParameters implements EventProcessorParameters {
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_NOTIFICATION_TYPE = "notification_type";
    private static final String FIELD_NOTIFICATION_MESSAGE = "notification_message";
    private static final String FIELD_NOTIFICATION_DETAILS = "notification_details";

    @JsonProperty(FIELD_TIMESTAMP)
    public abstract DateTime timestamp();

    @JsonProperty(FIELD_NOTIFICATION_TYPE)
    public abstract Notification.Type notificationType();

    @JsonProperty(FIELD_NOTIFICATION_MESSAGE)
    public abstract String notificationMessage();

    @JsonProperty(FIELD_NOTIFICATION_DETAILS)
    public abstract Map<String, Object> notificationDetails();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements EventProcessorParameters.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_SystemNotificationEventProcessorParameters.Builder()
                    .timestamp(DateTime.now(DateTimeZone.UTC))
                    .type(SystemNotificationEventProcessorConfig.TYPE_NAME)
                    .notificationDetails(new HashMap<>())
                    .notificationType(Notification.Type.GENERIC);
        }

        @JsonProperty(FIELD_TIMESTAMP)
        public abstract Builder timestamp(DateTime timestamp);

        @JsonProperty(FIELD_NOTIFICATION_TYPE)
        public abstract Builder notificationType(Notification.Type notificationType);

        @JsonProperty(FIELD_NOTIFICATION_MESSAGE)
        public abstract Builder notificationMessage(String details);

        @JsonProperty(FIELD_NOTIFICATION_DETAILS)
        public abstract Builder notificationDetails(Map<String, Object> details);

        public abstract SystemNotificationEventProcessorParameters build();
    }
}
