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
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
public abstract class RuleDao implements MongoEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_SOURCE = "source";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MODFIED_AT = "modfied_at";
    public static final String FIELD_RULEBUILDER = "rulebuilder";
    public static final String FIELD_SIMULATOR_MESSAGE = "simulator_message";

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

    @JsonProperty
    @Nullable
    public abstract RuleBuilder ruleBuilder();

    @JsonProperty
    @Nullable
    public abstract String simulatorMessage();

    public static Builder builder() {
        return new AutoValue_RuleDao.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static RuleDao create(@Id @ObjectId @JsonProperty(FIELD_ID) @Nullable String id,
                                 @JsonProperty(FIELD_TITLE) String title,
                                 @JsonProperty(FIELD_DESCRIPTION) @Nullable String description,
                                 @JsonProperty(FIELD_SOURCE) String source,
                                 @JsonProperty(FIELD_CREATED_AT) @Nullable DateTime createdAt,
                                 @JsonProperty(FIELD_MODFIED_AT) @Nullable DateTime modifiedAt,
                                 @JsonProperty(FIELD_RULEBUILDER) @Nullable RuleBuilder ruleBuilder,
                                 @JsonProperty(FIELD_SIMULATOR_MESSAGE) @Nullable String simulatorMessage) {
        return builder()
                .id(id)
                .source(source)
                .title(title)
                .description(description)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .ruleBuilder(ruleBuilder)
                .simulatorMessage(simulatorMessage)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RuleDao build();

        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder source(String source);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);

        public abstract Builder ruleBuilder(RuleBuilder ruleBuilder);

        public abstract Builder simulatorMessage(String simulatorMessage);
    }
}
