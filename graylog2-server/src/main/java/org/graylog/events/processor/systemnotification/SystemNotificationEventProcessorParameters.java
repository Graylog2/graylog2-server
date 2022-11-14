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
import org.graylog.events.processor.EventProcessorParametersWithTimerange;
import org.graylog2.notifications.Notification;
import org.graylog2.plugin.indexer.searches.timeranges.AbsoluteRange;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

@AutoValue
@JsonTypeName(SystemNotificationEventProcessorConfig.TYPE_NAME)
@JsonDeserialize(builder = SystemNotificationEventProcessorParameters.Builder.class)
public abstract class SystemNotificationEventProcessorParameters implements EventProcessorParametersWithTimerange {
    private static final String FIELD_TIMESTAMP = "timestamp";
    private static final String FIELD_NOTIFICATION_TYPE = "notification_type";
    private static final String FIELD_NOTIFICATION_DETAILS = "notification_details";

    @JsonProperty(FIELD_TIMESTAMP)
    public abstract DateTime timestamp();

    @JsonProperty(FIELD_NOTIFICATION_TYPE)
    public abstract Notification.Type notificationType();

    @JsonProperty(FIELD_NOTIFICATION_DETAILS)
    public abstract String notificationDetails();

    @Override
    public EventProcessorParametersWithTimerange withTimerange(DateTime from, DateTime to) {
        requireNonNull(from, "from cannot be null");
        requireNonNull(to, "to cannot be null");
        checkArgument(to.isAfter(from), "to must be after from");

        return toBuilder().timerange(AbsoluteRange.create(from, to)).build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public abstract static class Builder implements EventProcessorParametersWithTimerange.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            final RelativeRange timerange;
            try {
                timerange = RelativeRange.create(3600);
            } catch (InvalidRangeParametersException e) {
                // This should not happen!
                throw new RuntimeException(e);
            }

            return new AutoValue_SystemNotificationEventProcessorParameters.Builder()
                    .timestamp(DateTime.now(DateTimeZone.UTC))
                    .timerange(timerange)
                    .type(SystemNotificationEventProcessorConfig.TYPE_NAME)
                    .notificationType(Notification.Type.GENERIC);
        }

        @JsonProperty(FIELD_TIMESTAMP)
        public abstract Builder timestamp(DateTime timestamp);

        @JsonProperty(FIELD_NOTIFICATION_TYPE)
        public abstract Builder notificationType(Notification.Type notificationType);

        @JsonProperty(FIELD_NOTIFICATION_DETAILS)
        public abstract Builder notificationDetails(String details);

        public abstract SystemNotificationEventProcessorParameters build();
    }
}
