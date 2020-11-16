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
package org.graylog.events.notifications;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.contentpack.entities.EventNotificationConfigEntity;
import org.graylog.events.event.EventDto;
import org.graylog.scheduler.JobTriggerData;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.plugin.rest.ValidationResult;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventNotificationConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = EventNotificationConfig.FallbackNotificationConfig.class)
public interface EventNotificationConfig extends ContentPackable<EventNotificationConfigEntity> {
    String FIELD_NOTIFICATION_ID = "notification_id";
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    @JsonIgnore
    JobTriggerData toJobTriggerData(EventDto dto);

    @JsonIgnore
    ValidationResult validate();

    class FallbackNotificationConfig implements EventNotificationConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ValidationResult validate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public JobTriggerData toJobTriggerData(EventDto dto) {
            return null;
        }

        @Override
        public EventNotificationConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
            return null;
        }
    }
}
