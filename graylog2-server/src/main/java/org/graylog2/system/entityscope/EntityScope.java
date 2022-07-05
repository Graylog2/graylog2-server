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
package org.graylog2.system.entityscope;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

@AutoValue
@JsonDeserialize(builder = EntityScope.Builder.class)
public abstract class EntityScope {

    private static final String FIELD_ID = "id";
    public static final String FIELD_IS_MODIFIABLE = "is_modifiable";
    public static final String FIELD_NAME = "name";

    @Id
    @ObjectId
    @JsonProperty(FIELD_ID)
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public abstract String name();

    @JsonProperty(FIELD_IS_MODIFIABLE)
    public abstract boolean modifiable();

    public static Builder build() {
        return Builder.builder();
    }

    public Builder toBuilder() {
        return build()
                .id(id())
                .name(name())
                .modifiable(modifiable());
    }

    @AutoValue.Builder
    public abstract static class Builder {

        @Id
        @ObjectId
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_NAME)
        public abstract Builder name(String name);

        @JsonProperty(FIELD_IS_MODIFIABLE)
        public abstract Builder modifiable(boolean modifiable);

        public abstract EntityScope build();

        @JsonCreator
        public static Builder builder() {
            return new AutoValue_EntityScope.Builder()
                    .modifiable(true);
        }

        public static EntityScope createModifiable(String name) {
            return builder()
                    .name(name)
                    .modifiable(true)
                    .build();
        }

        public static EntityScope createUnmodifiable(String name) {
            return builder()
                    .name(name)
                    .modifiable(false)
                    .build();
        }

    }

}
