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
package org.graylog.events.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.Duration;

@JsonAutoDetect
@JsonDeserialize(builder = EventsConfiguration.Builder.class)
@AutoValue
public abstract class EventsConfiguration {
    private static final String FIELD_SEARCH_TIMEOUT = "events_search_timeout";
    private static final String FIELD_NOTIFICATIONS_RETRY_PERIOD = "events_notification_retry_period";
    private static final String FIELD_NOTIFICATIONS_DEFAULT_BACKLOG = "events_notification_default_backlog";
    private static final String FIELD_CATCHUP_WINDOW = "events_catchup_window";

    private static final long DEFAULT_SEARCH_TIMEOUT_MS = 60000;
    private static final long DEFAULT_NOTIFICATIONS_RETRY_MS = 300000;
    private static final long DEFAULT_NOTIFICATIONS_BACKLOG = 50;
    public static final long DEFAULT_CATCH_UP_WINDOW_MS = Duration.standardHours(1).getMillis();

    @JsonProperty(FIELD_SEARCH_TIMEOUT)
    public abstract long eventsSearchTimeout();

    @JsonProperty(FIELD_NOTIFICATIONS_RETRY_PERIOD)
    public abstract long eventNotificationsRetry();

    @JsonProperty(FIELD_NOTIFICATIONS_DEFAULT_BACKLOG)
    public abstract long eventNotificationsBacklog();

    @JsonProperty(FIELD_CATCHUP_WINDOW)
    public abstract long eventCatchupWindow();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventsConfiguration.Builder()
                    .eventsSearchTimeout(DEFAULT_SEARCH_TIMEOUT_MS)
                    .eventNotificationsRetry(DEFAULT_NOTIFICATIONS_RETRY_MS)
                    .eventNotificationsBacklog(DEFAULT_NOTIFICATIONS_BACKLOG)
                    .eventCatchupWindow(DEFAULT_CATCH_UP_WINDOW_MS);
        }

        @JsonProperty(FIELD_SEARCH_TIMEOUT)
        public abstract Builder eventsSearchTimeout(long searchTimeout);

        @JsonProperty(FIELD_NOTIFICATIONS_RETRY_PERIOD)
        public abstract Builder eventNotificationsRetry(long notificationRetry);

        @JsonProperty(FIELD_NOTIFICATIONS_DEFAULT_BACKLOG)
        public abstract Builder eventNotificationsBacklog(long defaultBacklog);

        @JsonProperty(FIELD_CATCHUP_WINDOW)
        public abstract Builder eventCatchupWindow(long catchupWindow);

        public abstract EventsConfiguration build();
    }
}
