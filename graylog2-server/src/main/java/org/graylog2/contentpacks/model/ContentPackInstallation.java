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
package org.graylog2.contentpacks.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.NativeEntityDescriptor;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.time.Instant;

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = AutoValue_ContentPackInstallation.Builder.class)
public abstract class ContentPackInstallation {
    public static final String FIELD_ID = "_id";
    public static final String FIELD_CONTENT_PACK_ID = "content_pack_id";
    public static final String FIELD_CONTENT_PACK_REVISION = "content_pack_revision";
    public static final String FIELD_PARAMETERS = "parameters";
    public static final String FIELD_ENTITIES = "entities";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_CREATED_BY = "created_by";

    @JsonProperty(FIELD_ID)
    @Nullable
    public abstract ObjectId id();

    @JsonProperty(FIELD_CONTENT_PACK_ID)
    public abstract ModelId contentPackId();

    @JsonProperty(FIELD_CONTENT_PACK_REVISION)
    public abstract int contentPackRevision();

    @JsonProperty(FIELD_PARAMETERS)
    public abstract ImmutableMap<String, ValueReference> parameters();

    @JsonProperty(FIELD_ENTITIES)
    public abstract ImmutableSet<NativeEntityDescriptor> entities();

    @JsonProperty(FIELD_COMMENT)
    public abstract String comment();

    @JsonProperty(FIELD_CREATED_AT)
    public abstract Instant createdAt();

    @JsonProperty(FIELD_CREATED_BY)
    public abstract String createdBy();

    public static Builder builder() {
        return new AutoValue_ContentPackInstallation.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonProperty(FIELD_ID)
        @Nullable
        abstract Builder id(ObjectId id);

        @JsonProperty(FIELD_CONTENT_PACK_ID)
        public abstract Builder contentPackId(ModelId contentPackId);

        @JsonProperty(FIELD_CONTENT_PACK_REVISION)
        public abstract Builder contentPackRevision(int contentPackRevision);

        @JsonProperty(FIELD_PARAMETERS)
        public abstract Builder parameters(ImmutableMap<String, ValueReference> parameters);

        @JsonProperty(FIELD_ENTITIES)
        public abstract Builder entities(ImmutableSet<NativeEntityDescriptor> entities);

        @JsonProperty(FIELD_COMMENT)
        public abstract Builder comment(String comment);

        @JsonProperty(FIELD_CREATED_AT)
        public abstract Builder createdAt(Instant createdAt);

        @JsonProperty(FIELD_CREATED_BY)
        public abstract Builder createdBy(String createdBy);

        abstract ContentPackInstallation autoBuild();

        public ContentPackInstallation build() {
            return autoBuild();
        }
    }
}
