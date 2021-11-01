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
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

@AutoValue
public abstract class PipelineDao {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODIFIED_AT = "modified_at";

    @JsonProperty(FIELD_ID)
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

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

    public boolean usesRule(String ruleTitle) {
        final Pattern p = Pattern.compile(f(".*rule\\s*\\\"%s\\\".*", ruleTitle), Pattern.DOTALL);
        return p.matcher(source()).matches();
    }

    public static Builder builder() {
        return new AutoValue_PipelineDao.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static PipelineDao create(@Id @ObjectId @JsonProperty("_id") @Nullable String id,
                                        @JsonProperty(FIELD_TITLE)  String title,
                                        @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                        @JsonProperty(FIELD_SOURCE) String source,
                                        @Nullable @JsonProperty(FIELD_CREATED_AT) DateTime createdAt,
                                        @Nullable @JsonProperty(FIELD_MODIFIED_AT) DateTime modifiedAt) {
        return builder()
                .id(id)
                .title(title)
                .description(description)
                .source(source)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PipelineDao build();

        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);
    }
}
