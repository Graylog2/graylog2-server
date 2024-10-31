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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.types.HTTPEventNotificationConfigV2;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.joda.time.DateTimeZone;

import java.util.Map;

@AutoValue
@JsonTypeName(HttpEventNotificationConfigV2Entity.TYPE_NAME)
@JsonDeserialize(builder = HttpEventNotificationConfigV2Entity.Builder.class)
public abstract class HttpEventNotificationConfigV2Entity implements EventNotificationConfigEntity {

    public static final String TYPE_NAME = "http-notification-v2";

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_URL)
    public abstract ValueReference url();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_SKIP_TLS_VERIFICATION)
    public abstract ValueReference skipTLSVerification();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_METHOD)
    public abstract ValueReference httpMethod();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_TIME_ZONE)
    public abstract ValueReference timeZone();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_CONTENT_TYPE)
    public abstract ValueReference contentType();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_HEADERS)
    public abstract ValueReference headers();

    @JsonProperty(HTTPEventNotificationConfigV2.FIELD_BODY_TEMPLATE)
    public abstract ValueReference bodyTemplate();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_HttpEventNotificationConfigV2Entity.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_URL)
        public abstract Builder url(ValueReference url);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_SKIP_TLS_VERIFICATION)
        public abstract Builder skipTLSVerification(ValueReference skipTLSVerification);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_METHOD)
        public abstract Builder httpMethod(ValueReference httpMethod);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_TIME_ZONE)
        public abstract Builder timeZone(ValueReference timeZone);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_CONTENT_TYPE)
        public abstract Builder contentType(ValueReference contentType);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_HEADERS)
        public abstract Builder headers(ValueReference headers);

        @JsonProperty(HTTPEventNotificationConfigV2.FIELD_BODY_TEMPLATE)
        public abstract Builder bodyTemplate(ValueReference bodyTemplate);

        public abstract HttpEventNotificationConfigV2Entity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return HTTPEventNotificationConfigV2.builder()
                .url(url().asString(parameters))
                .skipTLSVerification(skipTLSVerification().asBoolean(parameters))
                .httpMethod(HTTPEventNotificationConfigV2.HttpMethod.valueOf(httpMethod().asString(parameters)))
                .timeZone(DateTimeZone.forID(timeZone().asString(parameters)))
                .contentType(HTTPEventNotificationConfigV2.ContentType.valueOf(contentType().asString(parameters)))
                .headers(headers().asString(parameters))
                .bodyTemplate(bodyTemplate().asString(parameters))
                .build();
    }
}
