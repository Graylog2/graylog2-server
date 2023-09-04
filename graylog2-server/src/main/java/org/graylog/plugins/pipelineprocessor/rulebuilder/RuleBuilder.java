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
import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class RuleBuilder {

    protected static final String FIELD_OPERATOR = "operator";
    protected static final String FIELD_CONDITIONS = "conditions";
    protected static final String FIELD_ACTIONS = "actions";
    protected static final String FIELD_ERRORS = "errors";

    @JsonProperty(FIELD_OPERATOR)
    @Nullable
    public abstract RuleBuilderStep.Operator operator();

    @JsonProperty(FIELD_CONDITIONS)
    @Nullable
    public abstract List<RuleBuilderStep> conditions();

    @JsonProperty(FIELD_ACTIONS)
    @Nullable
    public abstract List<RuleBuilderStep> actions();

    @JsonProperty(FIELD_ERRORS)
    @Nullable
    public abstract List<String> errors();

    public static Builder builder() {
        return new AutoValue_RuleBuilder.Builder();
    }

    public abstract Builder toBuilder();

    @JsonCreator
    public static RuleBuilder create(@JsonProperty(FIELD_OPERATOR) @Nullable RuleBuilderStep.Operator operator,
                                     @JsonProperty(FIELD_CONDITIONS) @Nullable List<RuleBuilderStep> conditions,
                                     @JsonProperty(FIELD_ACTIONS) @Nullable List<RuleBuilderStep> actions,
                                     @JsonProperty(FIELD_ERRORS) @Nullable List<String> errors) {
        return builder()
                .operator(operator)
                .conditions(conditions)
                .actions(actions)
                .errors(errors)
                .build();
    }

    /**
     * Normalize data post-editing. In particular, renumber output variables starting from 1 in increments of 1.
     */
    public RuleBuilder normalize() {
        List<RuleBuilderStep> normalizedActions = new ArrayList<>();
        int outputSeq = 1;
        for (RuleBuilderStep action : actions()) {
            String outputVariable = action.outputvariable();
            if (action.isGeneratedOutput()) {
                outputVariable = action.generateOutput(outputSeq++);
            }
            normalizedActions.add(action.toBuilder().outputvariable(outputVariable).build());
        }

        return toBuilder()
                .actions(normalizedActions)
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract RuleBuilder build();

        public abstract Builder operator(RuleBuilderStep.Operator operator);

        public abstract Builder conditions(List<RuleBuilderStep> conditions);

        public abstract Builder actions(List<RuleBuilderStep> actions);

        public abstract Builder errors(List<String> errors);
    }

}
