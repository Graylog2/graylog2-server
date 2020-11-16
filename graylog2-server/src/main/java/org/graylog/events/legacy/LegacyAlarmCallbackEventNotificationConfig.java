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
package org.graylog.events.legacy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.LegacyAlarmCallbackEventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.Map;

@AutoValue
@JsonTypeName(LegacyAlarmCallbackEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = LegacyAlarmCallbackEventNotificationConfig.Builder.class)
public abstract class LegacyAlarmCallbackEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "legacy-alarm-callback-notification-v1";

    private static final String FIELD_CALLBACK_TYPE = "callback_type";
    private static final String FIELD_CONFIGURATION = "configuration";

    @JsonProperty(FIELD_CALLBACK_TYPE)
    public abstract String callbackType();

    @JsonProperty(FIELD_CONFIGURATION)
    public abstract Map<String, Object> configuration();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (callbackType().isEmpty()) {
            validation.addError(FIELD_CALLBACK_TYPE, "Legacy Notification callback type cannot be empty.");
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_LegacyAlarmCallbackEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_CALLBACK_TYPE)
        public abstract Builder callbackType(String callbackType);

        @JsonProperty(FIELD_CONFIGURATION)
        public abstract Builder configuration(Map<String, Object> configuration);

        public abstract LegacyAlarmCallbackEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return LegacyAlarmCallbackEventNotificationConfigEntity.builder()
            .callbackType(ValueReference.of(callbackType()))
            .configuration(configuration())
            .build();
    }
}
