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
import com.google.common.base.Strings;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.contentpack.entities.HttpEventNotificationConfigV2Entity;
import org.graylog.events.event.EventDto;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.EventNotificationExecutionJob;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.security.encryption.EncryptedValue;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
@JsonTypeName(HTTPEventNotificationConfigV2.TYPE_NAME)
@JsonDeserialize(builder = HTTPEventNotificationConfigV2.Builder.class)
public abstract class HTTPEventNotificationConfigV2 implements EventNotificationConfig {
    public static final String TYPE_NAME = "http-notification-v2";

    public static final String FIELD_URL = "url";
    public static final String FIELD_METHOD = "method";
    public static final String FIELD_TIME_ZONE = "time_zone";
    public static final String FIELD_CONTENT_TYPE = "content_type";
    public static final String FIELD_HEADERS = "headers";
    public static final String FIELD_BODY_TEMPLATE = "body_template";
    public static final String FIELD_SKIP_TLS_VERIFICATION = "skip_tls_verification";
    private static final String FIELD_BASIC_AUTH = "basic_auth";
    private static final String FIELD_API_KEY_AS_HEADER = "api_key_as_header";
    private static final String FIELD_API_KEY = "api_key";
    private static final String FIELD_API_SECRET = "api_secret";

    public enum HttpMethod {
        POST, PUT, GET
    }

    public enum ContentType {
        JSON, FORM_DATA, PLAIN_TEXT
    }

    @JsonProperty(FIELD_BASIC_AUTH)
    @Nullable
    public abstract EncryptedValue basicAuth();

    @JsonProperty(FIELD_API_KEY_AS_HEADER)
    public abstract boolean apiKeyAsHeader();

    @JsonProperty(FIELD_API_KEY)
    @Nullable
    public abstract String apiKey();

    @JsonProperty(FIELD_API_SECRET)
    @Nullable
    public abstract EncryptedValue apiSecret();

    @JsonProperty(FIELD_URL)
    public abstract String url();

    @JsonProperty(FIELD_SKIP_TLS_VERIFICATION)
    public abstract boolean skipTLSVerification();

    @JsonProperty(FIELD_METHOD)
    public abstract HttpMethod httpMethod();

    @JsonProperty(FIELD_TIME_ZONE)
    public abstract DateTimeZone timeZone();

    @JsonProperty(FIELD_CONTENT_TYPE)
    @Nullable
    public abstract ContentType contentType();

    @JsonProperty(FIELD_HEADERS)
    @Nullable
    public abstract String headers();

    @JsonProperty(FIELD_BODY_TEMPLATE)
    @Nullable
    public abstract String bodyTemplate();

    @Override
    @JsonIgnore
    public JobTriggerData toJobTriggerData(EventDto dto) {
        return EventNotificationExecutionJob.Data.builder().eventDto(dto).build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @Override
    @JsonIgnore
    public ValidationResult validate() {
        final ValidationResult validation = new ValidationResult();

        if (url().isEmpty()) {
            validation.addError(FIELD_URL, "HTTP Notification url cannot be empty.");
        }
        if (Strings.isNullOrEmpty(apiKey()) && (apiSecret() != null && apiSecret().isSet())) {
            validation.addError(FIELD_API_KEY, "HTTP Notification cannot specify API secret without API key");
        }
        if (!Strings.isNullOrEmpty(apiKey()) && (apiSecret() == null || (!apiSecret().isSet()) && !apiSecret().isKeepValue())) {
            validation.addError(FIELD_API_SECRET, "HTTP Notification cannot specify API key without API secret");
        }
        if (!Strings.isNullOrEmpty(headers())) {
            for (String nameVal : headers().split(";")) {
                String[] nameValArr = nameVal.split(":", 2);
                if (nameValArr.length != 2) {
                    validation.addError(FIELD_HEADERS, "Headers must be semi-colon delimited string in the form of 'Header1: Value1; Header2: Value2'");
                    break;
                }
                String name = nameValArr[0].trim();
                String val = nameValArr[1].trim();
                if (name.isEmpty() || val.isEmpty()) {
                    validation.addError(FIELD_HEADERS, "Header names and values cannot be empty.");
                    break;
                }
            }
        }
        if (!httpMethod().equals(HttpMethod.GET) && contentType() == null) {
            validation.addError(FIELD_CONTENT_TYPE, "Content Type must not be null for PUT/POST methods.");
        }
        if (contentType() == ContentType.FORM_DATA && !isNullOrEmpty(bodyTemplate())) {
            final String[] parts = bodyTemplate().split("&");
            for (String part : parts) {
                final int equalsIndex = part.indexOf("=");
                if (equalsIndex == -1) {
                    validation.addError(FIELD_BODY_TEMPLATE, "Invalid URL encoded form data template.");
                    break;
                }
                final String key = part.substring(0, equalsIndex);
                if (key.isEmpty()) {
                    validation.addError(FIELD_BODY_TEMPLATE, "Invalid URL encoded form data template.");
                    break;
                }
            }
        }
        return validation;
    }

    @Override
    @JsonIgnore
    public EventNotificationConfig prepareConfigUpdate(@Nonnull EventNotificationConfig newConfig) {
        final HTTPEventNotificationConfigV2 newHttpConfig = (HTTPEventNotificationConfigV2) newConfig;
        EncryptedValue newBasicAuth = newHttpConfig.basicAuth();
        if (newHttpConfig.basicAuth() != null) {
            if (newHttpConfig.basicAuth().isKeepValue()) {
                // If the client secret should be kept, use the value from the existing config
                newBasicAuth = basicAuth();
            }
            else if (newHttpConfig.basicAuth().isDeleteValue()) {
                newBasicAuth = EncryptedValue.createUnset();
            }
        }

        EncryptedValue newApiKeyValue = newHttpConfig.apiSecret();
        if (newHttpConfig.apiSecret() != null) {
            if (newHttpConfig.apiSecret().isKeepValue()) {
                // If the client secret should be kept, use the value from the existing config
                newApiKeyValue = apiSecret();
            }
            else if (newHttpConfig.apiSecret().isDeleteValue()) {
                newApiKeyValue = EncryptedValue.createUnset();
            }
        }

        return newHttpConfig.toBuilder().apiSecret(newApiKeyValue).basicAuth(newBasicAuth).build();
    }

    @AutoValue.Builder
    public abstract static class Builder implements EventNotificationConfig.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_HTTPEventNotificationConfigV2.Builder()
                    .basicAuth(EncryptedValue.createUnset())
                    .apiSecret(EncryptedValue.createUnset())
                    .apiKey("")
                    .apiKeyAsHeader(false)
                    .skipTLSVerification(false)
                    .type(TYPE_NAME)
                    .httpMethod(HttpMethod.POST)
                    .contentType(ContentType.JSON)
                    .bodyTemplate("")
                    .timeZone(DateTimeZone.UTC);
        }

        @JsonProperty(FIELD_BASIC_AUTH)
        public abstract Builder basicAuth(EncryptedValue basicAuth);

        @JsonProperty(FIELD_API_KEY_AS_HEADER)
        public abstract Builder apiKeyAsHeader(boolean apiKeyAsHeader);

        @JsonProperty(FIELD_API_KEY)
        public abstract Builder apiKey(String apiKey);

        @JsonProperty(FIELD_API_SECRET)
        public abstract Builder apiSecret(EncryptedValue apiKeyValue);

        @JsonProperty(FIELD_URL)
        public abstract Builder url(String url);

        @JsonProperty(FIELD_SKIP_TLS_VERIFICATION)
        public abstract Builder skipTLSVerification(boolean skip);

        @JsonProperty(FIELD_METHOD)
        public abstract Builder httpMethod(HttpMethod method);

        @JsonProperty(FIELD_TIME_ZONE)
        public abstract Builder timeZone(DateTimeZone timeZone);

        @JsonProperty(FIELD_CONTENT_TYPE)
        public abstract Builder contentType(ContentType contentType);

        @JsonProperty(FIELD_HEADERS)
        public abstract Builder headers(String headers);

        @JsonProperty(FIELD_BODY_TEMPLATE)
        public abstract Builder bodyTemplate(String bodyTemplate);

        public abstract HTTPEventNotificationConfigV2 build();
    }

    @Override
    public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        HttpEventNotificationConfigV2Entity.Builder builder = HttpEventNotificationConfigV2Entity.builder()
                .url(ValueReference.of(url()))
                .skipTLSVerification(ValueReference.of(skipTLSVerification()))
                .httpMethod(ValueReference.of(httpMethod()))
                .timeZone(ValueReference.of(timeZone().getID()))
                .headers(ValueReference.of(headers()));
        if (contentType() != null) {
            builder.contentType(ValueReference.of(contentType()));
            if (bodyTemplate() != null) {
                builder.bodyTemplate(ValueReference.of(bodyTemplate()));
            }
        }
        return builder.build();
    }
}
