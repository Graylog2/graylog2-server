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
package org.graylog.integrations.notifications.types.microsoftteams;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTimeZone;

import java.util.Map;

import static org.graylog.integrations.notifications.types.microsoftteams.TeamsEventNotificationConfigV2.DEFAULT_ADAPTIVE_CARD;
import static org.graylog.integrations.notifications.types.microsoftteams.TeamsEventNotificationConfigV2.DEFAULT_BACKLOG_SIZE;
import static org.graylog.integrations.notifications.types.microsoftteams.TeamsEventNotificationConfigV2.FIELD_ADAPTIVE_CARD;
import static org.graylog.integrations.notifications.types.microsoftteams.TeamsEventNotificationConfigV2.FIELD_TIME_ZONE;
import static org.graylog.integrations.notifications.types.microsoftteams.TeamsEventNotificationConfigV2.FIELD_WEBHOOK_URL;

@AutoValue
@JsonTypeName(TeamsEventNotificationConfigV2Entity.TYPE_NAME)
@JsonDeserialize(builder = TeamsEventNotificationConfigV2Entity.Builder.class)
public abstract class TeamsEventNotificationConfigV2Entity implements EventNotificationConfigEntity {

    public static final String TYPE_NAME = "teams-notification-v2";

    @JsonProperty(FIELD_WEBHOOK_URL)
    public abstract ValueReference webhookUrl();

    @JsonProperty(FIELD_ADAPTIVE_CARD)
    public abstract ValueReference adaptiveCard();

    @JsonProperty(FIELD_TIME_ZONE)
    public abstract ValueReference timeZone();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_TeamsEventNotificationConfigV2Entity.Builder()
                    .type(TYPE_NAME)
                    .adaptiveCard(ValueReference.of(DEFAULT_ADAPTIVE_CARD))
                    .timeZone(ValueReference.of("UTC"));
        }

        @JsonProperty(FIELD_WEBHOOK_URL)
        public abstract Builder webhookUrl(ValueReference webhookUrl);

        @JsonProperty(FIELD_ADAPTIVE_CARD)
        public abstract Builder adaptiveCard(ValueReference customMessage);

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(ValueReference timeZone);

        public abstract TeamsEventNotificationConfigV2Entity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return TeamsEventNotificationConfigV2.builder()
                .webhookUrl(webhookUrl().asString(parameters))
                .adaptiveCard(adaptiveCard().asString(parameters))
                .timeZone(DateTimeZone.forID(timeZone().asString(parameters)))
                .backlogSize(DEFAULT_BACKLOG_SIZE)
                .build();
    }
}

