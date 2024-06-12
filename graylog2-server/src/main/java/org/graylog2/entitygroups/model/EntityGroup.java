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
package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.entitygroups.contentpacks.entities.EntityGroupEntity;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.database.entities.ScopedEntity;

import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = EntityGroup.Builder.class)
@WithBeanGetter
public abstract class EntityGroup extends ScopedEntity implements ContentPackable<EntityGroupEntity> {
    public static final String FIELD_NAME = "name";
    public static final String FIELD_ENTITIES = "entities";

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_ENTITIES)
    public abstract Map<String, Set<String>> entities();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public abstract static class Builder extends AbstractBuilder<Builder> {
        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_ENTITIES)
        public abstract Builder entities(Map<String, Set<String>> entities);

        public abstract EntityGroup build();

        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityGroup.Builder();
        }
    }

    @Override
    public EntityGroupEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        return EntityGroupEntity.builder()
                .name(name())
                // Leave entities empty for now, they will need to be resolved with their respective ModelTypes in the facade.
                .entities(Map.of())
                .build();
    }
}
