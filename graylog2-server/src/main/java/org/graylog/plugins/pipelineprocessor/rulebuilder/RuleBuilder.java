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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
     * Normalize data post-editing
     */
    public RuleBuilder normalize() {
        if (Objects.isNull(actions()))
            return this;
        // Renumber generated output variables
        Map<Integer, Integer> varMapping = new HashMap<>();
        List<RuleBuilderStep> normalizedOutputs = new ArrayList<>();
        int newIndex = 1;
        for (RuleBuilderStep action : actions()) {
            String outputVariable = action.outputvariable();
            int varIndex = action.generatedOutputIndex();
            if (varIndex >= 0) {
                varMapping.put(varIndex, newIndex);
                outputVariable = action.generateOutput(newIndex++);
            }
            normalizedOutputs.add(action.toBuilder().outputvariable(outputVariable).build());
        }

        if (newIndex == 1) {
            return this; // no renumbering required
        }

        // Renumber parameters that are generated output variables.
        // These might be out of order, so we need to do it after renumbering all output variables.
        List<RuleBuilderStep> normalizedOutputsAndParams = new ArrayList<>();
        for (RuleBuilderStep action : normalizedOutputs) {
            Map<String, Object> updatedParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : action.parameters().entrySet()) {
                if (entry.getValue() instanceof String valueString) {
                    int paramIndex = action.generatedParameterIndex(valueString);
                    if (paramIndex > 0) {
                        updatedParams.put(entry.getKey(), action.generateParam(varMapping.get(paramIndex)));
                    } else {
                        updatedParams.put(entry.getKey(), entry.getValue());
                    }
                } else {
                    updatedParams.put(entry.getKey(), entry.getValue());
                }
            }
            normalizedOutputsAndParams.add(action.toBuilder().parameters(updatedParams).build());
        }

        return toBuilder()
                .actions(normalizedOutputsAndParams)
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
