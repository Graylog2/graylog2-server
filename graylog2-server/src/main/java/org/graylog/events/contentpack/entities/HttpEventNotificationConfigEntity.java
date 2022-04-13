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
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.util.Map;

@AutoValue
@JsonTypeName(HttpEventNotificationConfigEntity.TYPE_NAME)
@JsonDeserialize(builder = HttpEventNotificationConfigEntity.Builder.class)
public abstract class HttpEventNotificationConfigEntity implements EventNotificationConfigEntity {

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
    public abstract ValueReference url();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder implements EventNotificationConfigEntity.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_HttpEventNotificationConfigEntity.Builder()
                    .type(TYPE_NAME);
        }

        @JsonProperty(FIELD_BASIC_AUTH)
        public abstract Builder basicAuth(String basicAuth);

        @JsonProperty(FIELD_APIKEY)
        public abstract Builder apiKey(String apiKey);

        @JsonProperty(FIELD_APIKEY_VALUE)
        public abstract Builder apiKeyValue(String apiKeyValue);

        @JsonProperty(FIELD_URL)
        public abstract Builder url(ValueReference url);

        public abstract HttpEventNotificationConfigEntity build();
    }

    @Override
    public EventNotificationConfig toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return HTTPEventNotificationConfig.builder()
                .basicAuth(basicAuth())
                .apiKey(apiKey())
                .apiKeyValue(apiKeyValue())
                .url(url().asString(parameters))
                .build();
    }
}
