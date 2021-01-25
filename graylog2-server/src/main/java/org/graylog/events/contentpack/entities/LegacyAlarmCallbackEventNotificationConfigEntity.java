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
package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@AutoValue
@JsonDeserialize(builder = LegacyAlarmCallbackEventNotificationConfigEntity.Builder.class)
public abstract class LegacyAlarmCallbackEventNotificationConfigEntity implements EventNotificationConfigEntity {
    public static final String TYPE_NAME = "legacy-alarm-callback-notification-v1";

    private static final String FIELD_CALLBACK_TYPE = "callback_type";
    private static final String FIELD_CONFIGURATION = "configuration";

    @JsonProperty(FIELD_CALLBACK_TYPE)
    public abstract ValueReference callbackType();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> configuration();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_LegacyAlarmCallbackEventNotificationConfigEntity.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_CALLBACK_TYPE)
        public abstract Builder callbackType(ValueReference callbackType);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder configuration(Map<String, Object> configuration);


        public abstract LegacyAlarmCallbackEventNotificationConfigEntity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return LegacyAlarmCallbackEventNotificationConfig.builder()
                .callbackType(callbackType().asString(parameters))
                .configuration(configuration())
                .build();
    }
}
