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
package org.graylog.security.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;

import java.util.Collections;
import java.util.Set;

@AutoValue
@JsonDeserialize(builder = EntityDescriptor.Builder.class)
public abstract class EntityDescriptor {
    @JsonProperty("id")
    public abstract GRN id();

    @JsonProperty("type")
    public String type() {
        return id().type();
    }

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("owners")
    public abstract ImmutableSet<Owner> owners();

    public static EntityDescriptor create(GRN id, String title, Set<Owner> owners) {
        return builder()
                .id(id)
                .title(title)
                .owners(owners)
                .build();
    }

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_EntityDescriptor.Builder().owners(Collections.emptySet());
        }

        @JsonProperty("id")
        public abstract Builder id(GRN id);

        @JsonProperty("title")
        public abstract Builder title(String title);

        @JsonProperty("owners")
        public abstract Builder owners(Set<Owner> owners);

        public abstract EntityDescriptor build();
    }

    @AutoValue
    public static abstract class Owner {
        @JsonProperty("id")
        public abstract GRN id();

        @JsonProperty("type")
        public String type() {
            return id().type();
        }

        @JsonProperty("title")
        public abstract String title();

        @JsonCreator
        public static Owner create(GRN id, String title) {
            return new AutoValue_EntityDescriptor_Owner(id, title);
        }
    }
}
