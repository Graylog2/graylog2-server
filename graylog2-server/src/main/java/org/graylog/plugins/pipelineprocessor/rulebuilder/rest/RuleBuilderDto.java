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
package org.graylog.plugins.pipelineprocessor.rulebuilder.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class RuleBuilderDto {

    @JsonProperty
    @Nullable
    public abstract String id();

    @JsonProperty
    @Nullable
    public abstract String title();

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    @Nullable
    public abstract String source();

    @JsonProperty
    public abstract RuleBuilder ruleBuilder();

    @JsonProperty
    @Nullable
    public abstract DateTime createdAt();

    @JsonProperty
    @Nullable
    public abstract DateTime modifiedAt();

    @JsonProperty
    @Nullable
    public abstract String simulatorMessage();

    @JsonCreator
    public static RuleBuilderDto create(@JsonProperty("id") @Id @ObjectId @Nullable String id,
                                        @JsonProperty("title") String title,
                                        @JsonProperty("description") @Nullable String description,
                                        @JsonProperty("source") @Nullable String source,
                                        @JsonProperty("rule_builder") RuleBuilder ruleBuilder,
                                        @JsonProperty("created_at") @Nullable DateTime createdAt,
                                        @JsonProperty("modified_at") @Nullable DateTime modifiedAt,
                                        @JsonProperty("errors") @Nullable List<String> errors,
                                        @JsonProperty("simulator_message") @Nullable String simulatorMessage) {
        return builder()
                .id(id)
                .title(title)
                .description(description)
                .source(source)
                .ruleBuilder(ruleBuilder)
                .createdAt(createdAt)
                .modifiedAt(modifiedAt)
                .simulatorMessage(simulatorMessage)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RuleBuilderDto.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);

        public abstract Builder title(String title);

        public abstract Builder description(String description);

        public abstract Builder source(String source);

        public abstract Builder ruleBuilder(RuleBuilder ruleBuilder);

        public abstract Builder createdAt(DateTime createdAt);

        public abstract Builder modifiedAt(DateTime modifiedAt);

        public abstract Builder simulatorMessage(String simulatorMessage);

        public abstract RuleBuilderDto build();
    }

}
