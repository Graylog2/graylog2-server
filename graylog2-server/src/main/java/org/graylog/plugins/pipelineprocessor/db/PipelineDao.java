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
package org.graylog.plugins.pipelineprocessor.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.entities.DefaultEntityScope;
import org.graylog2.database.entities.ImmutableSystemScope;
import org.graylog2.database.entities.ScopedEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

import static org.graylog.plugins.pipelineprocessor.rest.PipelineResource.GL_INPUT_ROUTING_PIPELINE;

@AutoValue
public abstract class PipelineDao extends ScopedEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODIFIED_AT = "modified_at";

    @JsonProperty
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    public abstract String source();

    @JsonProperty
    @Nullable
    public abstract DateTime createdAt();

    @JsonProperty
    @Nullable
    public abstract DateTime modifiedAt();

    public static Builder builder() {
        return new AutoValue_PipelineDao.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static PipelineDao create(@Id @ObjectId @JsonProperty(FIELD_ID) @Nullable String id,
                                     @JsonProperty(FIELD_SCOPE) @Nullable String scope,
                                     @JsonProperty(FIELD_TITLE) String title,
                                     @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                     @JsonProperty(FIELD_SOURCE) String source,
                                     @Nullable @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
                                     @Nullable @JsonProperty(FIELD_MODIFIED_AT) DateTime modifiedAt) {
        if (title.equalsIgnoreCase(GL_INPUT_ROUTING_PIPELINE)) {
            scope = ImmutableSystemScope.NAME;
        } else if (scope == null) {
            scope = DefaultEntityScope.NAME;
        }
        return builder()
                .id(id)
                .scope(scope)
                .title(title)
                .description(description)
                .source(source)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder extends ScopedEntity.AbstractBuilder<Builder> {
        public abstract PipelineDao build();

        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);
    }
}
