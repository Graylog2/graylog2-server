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
import org.graylog.events.notifications.NotificationDto;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;

@AutoValue
@JsonDeserialize(builder = NotificationEntity.Builder.class)
public abstract class NotificationEntity implements NativeEntityConverter<NotificationDto> {

    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CONFIG = "config";

    public static Builder builder() {
        return Builder.create();
    }

    @JsonProperty(FIELD_TITLE)
    public abstract ValueReference title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract ValueReference description();

    @JsonProperty(FIELD_CONFIG)
    public abstract EventNotificationConfigEntity config();

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_NotificationEntity.Builder();
        }

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(ValueReference title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(ValueReference description);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(EventNotificationConfigEntity config);

        public abstract NotificationEntity build();
    }

    @Override
    public NotificationDto toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> nativeEntities) {
        return NotificationDto.builder()
                .description(description().asString(parameters))
                .title(title().asString(parameters))
                .config(config().toNativeEntity(parameters, nativeEntities))
                .build();
    }
}
