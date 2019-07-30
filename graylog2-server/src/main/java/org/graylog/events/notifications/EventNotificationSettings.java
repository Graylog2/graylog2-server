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
package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = EventNotificationSettings.Builder.class)
public abstract class EventNotificationSettings {
    public static final String FIELD_GRACE_PERIOD_MS = "grace_period_ms";
    public static final String FIELD_BACKLOG_SIZE = "backlog_size";

    @JsonProperty(FIELD_GRACE_PERIOD_MS)
    public abstract long gracePeriodMs();

    @JsonProperty(FIELD_BACKLOG_SIZE)
    public abstract long backlogSize();

    public static EventNotificationSettings withGracePeriod(long gracePeriodMs) {
        return builder().gracePeriodMs(gracePeriodMs).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventNotificationSettings.Builder().backlogSize(0L);
        }

        @JsonProperty(FIELD_GRACE_PERIOD_MS)
        public abstract Builder gracePeriodMs(long gracePeriodMs);

        @JsonProperty(FIELD_BACKLOG_SIZE)
        public abstract Builder backlogSize(long backlogSize);

        public abstract EventNotificationSettings build();
    }
}