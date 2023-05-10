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
package org.graylog.plugins.pipelineprocessor.rulebuilder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
public abstract class RuleBuilder {

    public static final String FIELD_CONDITIONS = "conditions";
    public static final String FIELD_ACTIONS = "actions";

    @JsonProperty(FIELD_CONDITIONS)
    @Nullable
    public abstract List<RuleBuilderStep> conditions();

    @JsonProperty(FIELD_ACTIONS)
    @Nullable
    public abstract List<RuleBuilderStep> actions();

    public static Builder builder() {
        return new AutoValue_RuleBuilder.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static RuleBuilder create(@JsonProperty(FIELD_CONDITIONS) @Nullable List<RuleBuilderStep> conditions,
                                     @JsonProperty(FIELD_ACTIONS) @Nullable List<RuleBuilderStep> actions) {
        return builder()
                .conditions(conditions)
                .actions(actions)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RuleBuilder build();

        public abstract Builder conditions(List<RuleBuilderStep> conditions);

        public abstract Builder actions(List<RuleBuilderStep> actions);
    }

}
