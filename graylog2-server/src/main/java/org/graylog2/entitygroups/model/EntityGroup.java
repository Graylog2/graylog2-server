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
import com.google.common.graph.MutableGraph;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.entitygroups.contentpacks.entities.EntityGroupEntity;
import org.graylog2.contentpacks.ContentPackable;
import org.graylog2.database.entities.ScopedEntity;

import java.util.HashMap;
import java.util.HashSet;
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

    public EntityGroup addEntity(String type, String entityId) {
        final Map<String, Set<String>> entities = entities() != null ? new HashMap<>(entities()) : new HashMap<>();
        final Set<String> entityIds = entities.get(type) != null ? new HashSet<>(entities.get(type)) : new HashSet<>();

        entityIds.add(entityId);
        entities.put(type, entityIds);
        return this.toBuilder().entities(entities).build();
    }

    @Override
    public EntityGroupEntity toContentPackEntity(EntityDescriptorIds entityDescriptorIds) {
        // TODO: Resolve all native entities referenced in the entities map and link content pack IDs.
        // TODO: Should we export all entities referenced in this group as dependencies when a group is added to a content pack?
        return null;
    }

    @Override
    public void resolveNativeEntity(EntityDescriptor entityDescriptor, MutableGraph<EntityDescriptor> mutableGraph) {
        // TODO: Resolve any linkages to entities in the entities map.
    }
}
