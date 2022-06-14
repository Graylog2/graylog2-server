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

import java.util.Map;

@AutoValue
@JsonTypeName(TeamsEventNotificationConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = TeamsEventNotificationConfigEntity.Builder.class)
public abstract class TeamsEventNotificationConfigEntity implements EventNotificationConfigEntity {

    public static final String TYPE_NAME = "teams-notification-v1";

    @JsonProperty(TeamsEventNotificationConfig.TEAMS_COLOR)
    public abstract ValueReference color();

    @JsonProperty(TeamsEventNotificationConfig.FIELD_WEBHOOK_URL)
    public abstract ValueReference webhookUrl();

    @JsonProperty(TeamsEventNotificationConfig.TEAMS_CUSTOM_MESSAGE)
    public abstract ValueReference customMessage();

    @JsonProperty(TeamsEventNotificationConfig.TEAMS_ICON_URL)
    public abstract ValueReference iconUrl();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_TeamsEventNotificationConfigEntity.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(TeamsEventNotificationConfig.TEAMS_COLOR)
        public abstract Builder color(ValueReference color);

        @JsonProperty(TeamsEventNotificationConfig.FIELD_WEBHOOK_URL)
        public abstract Builder webhookUrl(ValueReference webhookUrl);

        @JsonProperty(TeamsEventNotificationConfig.TEAMS_CUSTOM_MESSAGE)
        public abstract Builder customMessage(ValueReference customMessage);

        @JsonProperty(TeamsEventNotificationConfig.TEAMS_ICON_URL)
        public abstract Builder iconUrl(ValueReference iconUrl);

        public abstract TeamsEventNotificationConfigEntity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return TeamsEventNotificationConfig.builder()
                .color(color().asString(parameters))
                .webhookUrl(webhookUrl().asString(parameters))
                .customMessage(customMessage().asString(parameters))
                .customMessage(customMessage().asString(parameters))
                .iconUrl(iconUrl().asString(parameters))
                .build();
    }
}

