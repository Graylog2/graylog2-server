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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder.FIELD_ERRORS;

@AutoValue
public abstract class RuleBuilderStep {

    public static final String FIELD_ID = "id";
    public static final String FIELD_FUNCTION = "function";
    public static final String FIELD_PARAMETERS = "params";
    public static final String FIELD_OUTPUT = "outputvariable";
    public static final String FIELD_NEGATE = "negate";
    public static final String FIELD_TITLE = "step_title";
    public static final String FIELD_OPERATOR = "operator";
    public static final String FIELD_NESTED_CONDITIONS = "conditions";

    private static final String OUTPUT_PREFIX = "output_";
    private static final Pattern OUTPUT_PATTERN = Pattern.compile(OUTPUT_PREFIX + "(\\d+)");
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\$" + OUTPUT_PREFIX + "(\\d+)");

    @JsonProperty(FIELD_ID)
    @Nullable
    @ObjectId
    public abstract String id();

    @JsonProperty(FIELD_FUNCTION)
    @Nullable
    public abstract String function();

    @JsonProperty(FIELD_PARAMETERS)
    @Nullable
    public abstract Map<String, Object> parameters();

    @JsonProperty(FIELD_OUTPUT)
    @Nullable
    public abstract String outputvariable();

    @JsonProperty(FIELD_NEGATE)
    public abstract boolean negate();

    @JsonProperty(FIELD_TITLE)
    @Nullable
    public abstract String title();

    @JsonProperty(FIELD_OPERATOR)
    @Nullable
    public abstract Operator operator();

    @JsonProperty(FIELD_NESTED_CONDITIONS)
    @Nullable
    public abstract List<RuleBuilderStep> conditions();

    @JsonProperty(FIELD_ERRORS)
    @Nullable
    public abstract List<String> errors();

    @JsonCreator
    public static RuleBuilderStep create(@JsonProperty(FIELD_ID) @Nullable String id,
                                         @JsonProperty(FIELD_FUNCTION) @Nullable String function,
                                         @JsonProperty(FIELD_PARAMETERS) @Nullable Map<String, Object> parameters,
                                         @JsonProperty(FIELD_OUTPUT) @Nullable String outputvariable,
                                         @JsonProperty(FIELD_NEGATE) @Nullable boolean negate,
                                         @JsonProperty(FIELD_TITLE) @Nullable String title,
                                         @JsonProperty(FIELD_OPERATOR) @Nullable Operator operator,
                                         @JsonProperty(FIELD_NESTED_CONDITIONS) @Nullable List<RuleBuilderStep> conditions,
                                         @JsonProperty(FIELD_ERRORS) @Nullable List<String> errors) {
        return builder()
                .id(id)
                .function(function)
                .parameters(parameters)
                .outputvariable(outputvariable)
                .negate(negate)
                .title(title)
                .operator(operator)
                .conditions(conditions)
                .errors(errors)
                .build();
    }

    public static Builder builder() {
        return new AutoValue_RuleBuilderStep.Builder().negate(false);
    }

    public abstract Builder toBuilder();

    @JsonIgnore
    public int generatedOutputIndex() {
        return matchPatternWithIndex(outputvariable(), OUTPUT_PATTERN);
    }

    @JsonIgnore
    public int generatedParameterIndex(String parameter) {
        return matchPatternWithIndex(parameter, PARAMETER_PATTERN);
    }

    @JsonIgnore
    private int matchPatternWithIndex(String value, Pattern pattern) {
        if (value == null) {
            return -1;
        }
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(1));
        }
        return -1;
    }

    @JsonIgnore
    public String generateOutput(int index) {
        return OUTPUT_PREFIX + index;
    }

    @JsonIgnore
    public String generateParam(int index) {
        return "$" + OUTPUT_PREFIX + index;
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String id);

        public abstract Builder function(String function);

        public abstract Builder parameters(Map<String, Object> parameters);

        public abstract Builder outputvariable(String outputvariable);

        public abstract Builder negate(boolean negate);

        public Builder negate() {
            return negate(true);
        }

        public abstract Builder title(String title);

        public abstract Builder operator(Operator operator);

        public abstract Builder conditions(List<RuleBuilderStep> conditions);

        public abstract Builder errors(List<String> errors);

        public abstract RuleBuilderStep build();

    }

    public enum Operator {
        AND, OR
    }

}
