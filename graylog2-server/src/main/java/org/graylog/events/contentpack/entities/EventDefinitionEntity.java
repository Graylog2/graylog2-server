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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.graph.MutableGraph;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog2.contentpacks.NativeEntityConverter;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@AutoValue
@JsonDeserialize(builder = EventDefinitionEntity.Builder.class)
public abstract class EventDefinitionEntity implements NativeEntityConverter<EventDefinitionDto> {
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_PRIORITY = "priority";
    private static final String FIELD_ALERT = "alert";
    private static final String FIELD_CONFIG = "config";
    private static final String FIELD_FIELD_SPEC = "field_spec";
    private static final String FIELD_KEY_SPEC = "key_spec";
    private static final String FIELD_NOTIFICATION_SETTINGS = "notification_settings";
    private static final String FIELD_NOTIFICATIONS = "notifications";
    private static final String FIELD_STORAGE = "storage";
    private static final String FIELD_IS_SCHEDULED = "is_scheduled";

    @JsonProperty(FIELD_TITLE)
    public abstract ValueReference title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract ValueReference description();

    @JsonProperty(FIELD_PRIORITY)
    public abstract ValueReference priority();

    @JsonProperty(FIELD_ALERT)
    public abstract ValueReference alert();

    @JsonProperty(FIELD_CONFIG)
    public abstract EventProcessorConfigEntity config();

    @JsonProperty(FIELD_FIELD_SPEC)
    public abstract ImmutableMap<String, EventFieldSpec> fieldSpec();

    @JsonProperty(FIELD_KEY_SPEC)
    public abstract ImmutableList<String> keySpec();

    @JsonProperty(FIELD_NOTIFICATION_SETTINGS)
    public abstract EventNotificationSettings notificationSettings();

    @JsonProperty(FIELD_NOTIFICATIONS)
    public abstract ImmutableList<EventNotificationHandlerConfigEntity> notifications();

    @JsonProperty(FIELD_STORAGE)
    public abstract ImmutableList<EventStorageHandler.Config> storage();

    @JsonProperty(FIELD_IS_SCHEDULED)
    public abstract ValueReference isScheduled();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EventDefinitionEntity.Builder().isScheduled(ValueReference.of(true));
        }

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(ValueReference title);

        @JsonProperty(FIELD_DESCRIPTION)
        public abstract Builder description(ValueReference description);

        @JsonProperty(FIELD_PRIORITY)
        public abstract Builder priority(ValueReference priority);

        @JsonProperty(FIELD_ALERT)
        public abstract Builder alert(ValueReference alert);

        @JsonProperty(FIELD_CONFIG)
        public abstract Builder config(EventProcessorConfigEntity config);

        @JsonProperty(FIELD_FIELD_SPEC)
        public abstract Builder fieldSpec(ImmutableMap<String, EventFieldSpec> fieldSpec);

        @JsonProperty(FIELD_KEY_SPEC)
        public abstract Builder keySpec(ImmutableList<String> keySpec);

        @JsonProperty(FIELD_NOTIFICATION_SETTINGS)
        public abstract Builder notificationSettings(EventNotificationSettings notificationSettings);

        @JsonProperty(FIELD_NOTIFICATIONS)
        public abstract Builder notifications(ImmutableList<EventNotificationHandlerConfigEntity> notifications);

        @JsonProperty(FIELD_STORAGE)
        public abstract Builder storage(ImmutableList<EventStorageHandler.Config> storage);

        @JsonProperty(FIELD_IS_SCHEDULED)
        public abstract Builder isScheduled(ValueReference isScheduled);

        public abstract EventDefinitionEntity build();
    }

    @Override
    public EventDefinitionDto toNativeEntity(Map<String, ValueReference> parameters, Map<EntityDescriptor, Object> natvieEntities) {
        final ImmutableList<EventNotificationHandler.Config> notificationList = ImmutableList.copyOf(
                notifications().stream()
                        .map(notification -> notification.toNativeEntity(parameters, natvieEntities))
                        .collect(Collectors.toList())
        );
        return EventDefinitionDto.builder()
                .title(title().asString(parameters))
                .description(description().asString(parameters))
                .priority(priority().asInteger(parameters))
                .alert(alert().asBoolean(parameters))
                .config(config().toNativeEntity(parameters, natvieEntities))
                .fieldSpec(fieldSpec())
                .keySpec(keySpec())
                .notificationSettings(notificationSettings())
                .notifications(notificationList)
                .storage(storage())
                .build();
    }

    @Override
    public void resolveForInstallation(EntityV1 entity, Map<String, ValueReference> parameters, Map<EntityDescriptor, Entity> entities, MutableGraph<Entity> graph) {
        notifications().stream()
                .map(EventNotificationHandlerConfigEntity::notificationId)
                .map(valueReference -> valueReference.asString(parameters))
                .map(ModelId::of)
                .map(modelId -> EntityDescriptor.create(modelId, ModelTypes.NOTIFICATION_V1))
                .map(entities::get)
                .filter(Objects::nonNull)
                .forEach(notification -> graph.putEdge(entity, notification));

        config().resolveForInstallation(entity, parameters, entities, graph);
    }
}
