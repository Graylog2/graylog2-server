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
package org.graylog.events.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.contentpack.entities.EventProcessorConfigEntity;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.plugin.rest.ValidationResult;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = EventProcessorConfig.TYPE_FIELD,
        visible = true,
        defaultImpl = EventProcessorConfig.FallbackConfig.class)
public interface EventProcessorConfig extends ContentPackable<EventProcessorConfigEntity> {
    String TYPE_FIELD = "type";

    @JsonProperty(TYPE_FIELD)
    String type();

    /**
     * Returns a {@link JobDefinitionConfig} for this event processor configuration. If the event processor shouldn't
     * be scheduled, this method returns an empty {@link Optional}.
     *
     * @param eventDefinition the event definition
     * @param clock           the clock that can be used to get the current time
     * @return the job definition config or an empty optional if the processor shouldn't be scheduled
     */
    @JsonIgnore
    default Optional<EventProcessorSchedulerConfig> toJobSchedulerConfig(EventDefinition eventDefinition, JobSchedulerClock clock) {
        return Optional.empty();
    }

    /**
     * Validates the event processor configuration.
     *
     * @return the validation result
     */
    @JsonIgnore
    ValidationResult validate();

    /**
     * Returns the permissions that are required to create the event processor configuration. (e.g. stream permissions)
     *
     * @return the required permissions
     */
    @JsonIgnore
    default Set<String> requiredPermissions() {
        return Collections.emptySet();
    }

    interface Builder<SELF> {
        @JsonProperty(TYPE_FIELD)
        SELF type(String type);
    }

    class FallbackConfig implements EventProcessorConfig {
        @Override
        public String type() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ValidationResult validate() {
            throw new UnsupportedOperationException();
        }

        @Override
        public EventProcessorConfigEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
            return null;
        }
    }
}
