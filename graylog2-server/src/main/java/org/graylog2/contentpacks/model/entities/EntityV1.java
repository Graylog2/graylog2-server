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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.ModelVersion;
import org.graylog2.contentpacks.model.constraints.Constraint;
import org.graylog2.contentpacks.model.constraints.GraylogVersionConstraint;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_EntityV1.Builder.class)
public abstract class EntityV1 implements Entity {
    public static final String VERSION = "1";
    public static final String FIELD_DATA = "data";
    public static final String FIELD_CONSTRAINTS = "constraints";

    // TODO: Use more type-safe way to represent entity configuration?
    @JsonProperty(FIELD_DATA)
    public abstract JsonNode data();

    @JsonProperty(FIELD_CONSTRAINTS)
    public abstract ImmutableSet<Constraint> constraints();

    @Override
    public EntityDescriptor toEntityDescriptor() {
        return EntityDescriptor.builder()
                .id(id())
                .type(type())
                .build();
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_EntityV1.Builder()
                .constraints(ImmutableSet.<Constraint>builder().
                        add(GraylogVersionConstraint.currentGraylogVersion()).build());
    }

    public static Entity createRoot(ContentPack contentPack) {
        return EntityV1.builder()
                .type(ModelTypes.ROOT)
                .id(ModelId.of("virtual-root-" + contentPack.id() + "-" + contentPack.revision()))
                .data(NullNode.getInstance())
                .constraints(ImmutableSet.of())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder implements EntityBuilder<Builder> {
        @JsonProperty(FIELD_DATA)
        public abstract Builder data(JsonNode data);

        @JsonProperty(FIELD_CONSTRAINTS)
        public abstract Builder constraints(ImmutableSet<Constraint> constraints);

        abstract EntityV1 autoBuild();

        public EntityV1 build() {
            version(ModelVersion.of(VERSION));

            final EntityV1 entityV1 = autoBuild();

            // Make sure we always include a server version constraint
            if (missesServerConstraint(entityV1)) {
                return entityV1.toBuilder()
                        .constraints(ImmutableSet.<Constraint>builder()
                                .addAll(entityV1.constraints())
                                .add(GraylogVersionConstraint.currentGraylogVersion())
                                .build())
                        .build();
            }

            return entityV1;
        }

        /* Checks if the server constraint is included in the entity already.
           Two Constraint objects with different versions are not equal so we can't just
           use the properties of the set and have to check if a constraint with the server type
           is already included. */
        private boolean missesServerConstraint(EntityV1 entityV1) {
            return entityV1.constraints().stream()
                    .map(Constraint::type)
                    .noneMatch(GraylogVersionConstraint.TYPE_NAME::equals);
        }
    }
}
