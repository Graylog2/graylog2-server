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
package org.graylog.events.notifications.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import joptsimple.internal.Strings;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;

import javax.annotation.Nullable;

@AutoValue
@JsonTypeName(HTTPEventNotificationConfig.TYPE_NAME)
@JsonDeserialize(builder = HTTPEventNotificationConfig.Builder.class)
public abstract class HTTPEventNotificationConfig implements EventNotificationConfig {
    public static final String TYPE_NAME = "http-notification-v1";

    private static final String FIELD_URL = "url";
    private static final String FIELD_BASIC_AUTH = "basic_auth";
    private static final String FIELD_APIKEY = "apikey";
    private static final String FIELD_APIKEY_VALUE = "apikey_value";

    @JsonProperty(FIELD_BASIC_AUTH)
    @Nullable
    public abstract String basicAuth();

    @JsonProperty(FIELD_APIKEY)
    @Nullable
    public abstract String apiKey();

    @JsonProperty(FIELD_APIKEY_VALUE)
    @Nullable
    public abstract String apiKeyValue();

    @JsonProperty(FIELD_URL)
    public abstract String url();

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

        if (url().isEmpty()) {
            validation.addError(FIELD_URL, "HTTP Notification url cannot be empty.");
        }
        if (Strings.isNullOrEmpty(apiKey()) && !Strings.isNullOrEmpty(apiKeyValue())) {
            validation.addError(FIELD_APIKEY, "HTTP Notification cannot specify API key value without API key");
        }
        if (!Strings.isNullOrEmpty(apiKey()) && Strings.isNullOrEmpty(apiKeyValue())) {
            validation.addError(FIELD_APIKEY_VALUE, "HTTP Notification cannot specify API key without API key value");
        }

        return validation;
    }

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_HTTPEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_BASIC_AUTH)
        @Nullable
        public abstract Builder basicAuth(String basicAuth);

        @JsonProperty(FIELD_APIKEY)
        @Nullable
        public abstract Builder apiKey(String apiKey);

        @JsonProperty(FIELD_APIKEY_VALUE)
        @Nullable
        public abstract Builder apiKeyValue(String apiKeyValue);

        @JsonProperty(FIELD_URL)
        public abstract Builder url(String url);

        public abstract HTTPEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return HttpEventNotificationConfigEntity.builder()
                .basicAuth(basicAuth())
                .apiKey(apiKey())
                .apiKeyValue(apiKeyValue())
                .url(ValueReference.of(url()))
                .build();
    }
}
