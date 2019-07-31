/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.events.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

import org.joda.time.Period;

@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_EventsConfiguration.Builder.class)
@AutoValue
public abstract class EventsConfiguration {
    private static final String FIELD_SEARCH_TIMEOUT = "events_search_timeout";
    private static final String FIELD_NOTIFICATIONS_RETRY_PERIOD = "events_notification_retry_period";
    private static final String FIELD_NOTIFICATIONS_DEFAULT_BACKLOG = "events_notification_default_backlog";

    private static final Period DEFAULT_SEARCH_TIMEOUT = Period.seconds(60);
    private static final Period DEFAULT_NOTIFICATIONS_RETRY_PERIOD = Period.minutes(5);
    private static final long DEFAULT_NOTIFICATIONS_BACKLOG = 50;

    @JsonProperty(FIELD_SEARCH_TIMEOUT)
    public abstract Period eventsSearchTimeout();

    @JsonProperty(FIELD_NOTIFICATIONS_RETRY_PERIOD)
    public abstract Period eventNotificationsRetry();

    @JsonProperty(FIELD_NOTIFICATIONS_DEFAULT_BACKLOG)
    public abstract long eventNotificationsBacklog();

    public static Builder builder() {
        return new AutoValue_EventsConfiguration.Builder()
                .eventsSearchTimeout(DEFAULT_SEARCH_TIMEOUT)
                .eventNotificationsRetry(DEFAULT_NOTIFICATIONS_RETRY_PERIOD)
                .eventNotificationsBacklog(DEFAULT_NOTIFICATIONS_BACKLOG);
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_SEARCH_TIMEOUT)
        public abstract Builder eventsSearchTimeout(Period searchTimeout);

        @JsonProperty(FIELD_NOTIFICATIONS_RETRY_PERIOD)
        public abstract Builder eventNotificationsRetry(Period notificationRetry);

        @JsonProperty(FIELD_NOTIFICATIONS_DEFAULT_BACKLOG)
        public abstract Builder eventNotificationsBacklog(long defaultBacklog);

        public abstract EventsConfiguration build();
    }
}
