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
package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EventNotificationStatus.Builder.class)
public abstract class EventNotificationStatus {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NOTIFICATION_ID = "notification_id";
    public static final String FIELD_EVENT_DEFINITION_ID = "event_definition_id";
    public static final String FIELD_EVENT_KEY = "event_key";
    public static final String FIELD_TRIGGERED_AT = "triggered_at";
    private static final String FIELD_NOTIFIED_AT = "notified_at";

    @Id
    @ObjectId
    @Nullable
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NOTIFICATION_ID)
    public abstract String notificationId();

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    public abstract String eventDefinitionId();

    @JsonProperty(FIELD_EVENT_KEY)
    public abstract String eventKey();

    @JsonProperty(EventNotificationSettings.FIELD_GRACE_PERIOD_MS)
    public abstract long gracePeriodMs();

    @JsonProperty(FIELD_TRIGGERED_AT)
    public abstract Optional<DateTime> triggeredAt();

    @JsonProperty(FIELD_NOTIFIED_AT)
    public abstract Optional<DateTime> notifiedAt();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventNotificationStatus.Builder();
        }

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_NOTIFICATION_ID)
        public abstract Builder notificationId(String notificationId);

        @JsonProperty(FIELD_EVENT_DEFINITION_ID)
        public abstract Builder eventDefinitionId(String eventDefinitionId);

        @JsonProperty(FIELD_EVENT_KEY)
        public abstract Builder eventKey(String eventKey);

        @JsonProperty(EventNotificationSettings.FIELD_GRACE_PERIOD_MS)
        public abstract Builder gracePeriodMs(long gracePeriodMs);

        @JsonProperty(FIELD_TRIGGERED_AT)
        public abstract Builder triggeredAt(Optional<DateTime> triggeredAt);

        @JsonProperty(FIELD_NOTIFIED_AT)
        public abstract Builder notifiedAt(Optional<DateTime> notifiedAt);

        public abstract EventNotificationStatus build();
    }
}
