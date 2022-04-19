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
import org.graylog2.security.encryption.EncryptedValue;

import javax.annotation.Nonnull;
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
    public abstract EncryptedValue basicAuth();

    @JsonProperty(FIELD_APIKEY)
    @Nullable
    public abstract String apiKey();

    @JsonProperty(FIELD_APIKEY_VALUE)
    @Nullable
    public abstract EncryptedValue apiKeyValue();

    @JsonProperty(FIELD_URL)
    public abstract String url();

    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();
    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (url().isEmpty()) {
            validation.addError(FIELD_URL, "HTTP Notification url cannot be empty.");
        }
        if (Strings.isNullOrEmpty(apiKey()) && (apiKeyValue() != null) && apiKeyValue().isSet()) {
            validation.addError(FIELD_APIKEY, "HTTP Notification cannot specify API key value without API key");
        }
        if (!Strings.isNullOrEmpty(apiKey()) && (apiKeyValue() == null)) {
            validation.addError(FIELD_APIKEY_VALUE, "HTTP Notification cannot specify API key without API key value");
        }

        return validation;
    }

    @Override
    @JsonIgnore
    public EventNotificationConfig prepareConfigUpdate(@Nonnull EventNotificationConfig newConfig) {
        final HTTPEventNotificationConfig newHttpConfig = (HTTPEventNotificationConfig) newConfig;
        if (newHttpConfig.basicAuth() != null && newHttpConfig.basicAuth().isDeleteValue()) {
            return newHttpConfig.toBuilder().basicAuth(null).build();
        }
        if (newHttpConfig.apiKeyValue() != null && newHttpConfig.apiKeyValue().isKeepValue()) {
            // If the client secret should be kept, use the value from the existing config
            return newHttpConfig.toBuilder().apiKeyValue(apiKeyValue()).build();
        }
        return newHttpConfig;
    }

    @AutoValue.Builder
    public abstract static class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_HTTPEventNotificationConfig.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_BASIC_AUTH)
        public abstract Builder basicAuth(EncryptedValue basicAuth);

        @JsonProperty(FIELD_APIKEY)
        public abstract Builder apiKey(String apiKey);

        @JsonProperty(FIELD_APIKEY_VALUE)
        public abstract Builder apiKeyValue(EncryptedValue apiKeyValue);

        @JsonProperty(FIELD_URL)
        public abstract Builder url(String url);

        public abstract HTTPEventNotificationConfig build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return HttpEventNotificationConfigEntity.builder()
                .url(ValueReference.of(url()))
                .build();
    }
}
