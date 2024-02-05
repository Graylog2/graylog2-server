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
package org.graylog.plugins.pipelineprocessor.ast.functions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderFunctionGroup;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class FunctionDescriptor<T> {

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract boolean pure();

    @JsonProperty
    public abstract Class<? extends T> returnType();

    @JsonProperty
    public abstract ImmutableList<ParameterDescriptor> params();

    @JsonIgnore
    public abstract ImmutableMap<String, ParameterDescriptor> paramMap();

    @JsonIgnore
    public ParameterDescriptor param(String name) {
        return paramMap().get(name);
    }

    @JsonProperty
    @Nullable
    public abstract String description();

    @JsonProperty
    public abstract boolean ruleBuilderEnabled();

    @JsonIgnore
    @Nullable
    public abstract String ruleBuilderName();

    /**
     * default to function name
     *
     * @return function name, if rule builder is enabled and no name is explicitly set
     */
    @JsonProperty("rule_builder_name")
    public String getRuleBuilderName() {
        if (ruleBuilderEnabled() && ruleBuilderName() == null) {
            return name();
        }
        return ruleBuilderName();
    }

    @JsonProperty
    @Nullable
    public abstract String ruleBuilderTitle();

    @JsonIgnore
    @Nullable
    public abstract RuleBuilderFunctionGroup ruleBuilderFunctionGroup();

    public static <T> Builder<T> builder() {
        //noinspection unchecked
        return new AutoValue_FunctionDescriptor.Builder().pure(false).ruleBuilderEnabled(false);
    }

    @AutoValue.Builder
    public static abstract class Builder<T> {
        abstract FunctionDescriptor<T> autoBuild();

        public FunctionDescriptor<T> build() {
            return paramMap(Maps.uniqueIndex(params(), ParameterDescriptor::name))
                    .autoBuild();
        }

        public abstract Builder<T> name(String name);

        public abstract Builder<T> pure(boolean pure);

        public abstract Builder<T> returnType(Class<? extends T> type);

        public abstract Builder<T> ruleBuilderEnabled(boolean ruleBuilderEnabled);

        public Builder<T> ruleBuilderEnabled() {
            return ruleBuilderEnabled(true);
        }

        public abstract Builder<T> ruleBuilderName(String ruleBuilderName);

        public abstract Builder<T> ruleBuilderTitle(String ruleBuilderTitle);

        public abstract Builder<T> ruleBuilderFunctionGroup(RuleBuilderFunctionGroup ruleBuilderFunctionGroup);

        public Builder<T> params(ParameterDescriptor... params) {
            return params(ImmutableList.<ParameterDescriptor>builder().add(params).build());
        }

        public abstract Builder<T> params(ImmutableList<ParameterDescriptor> params);

        public abstract Builder<T> paramMap(ImmutableMap<String, ParameterDescriptor> map);

        public abstract ImmutableList<ParameterDescriptor> params();

        public abstract Builder<T> description(@Nullable String description);
    }

    @JsonCreator
    public static <T> FunctionDescriptor<T> createForRuleBuilder(
            @JsonProperty("name") String name,
            @JsonProperty("return_type") Class<? extends T> returnType,
            @JsonProperty("params") @Nullable ImmutableList<ParameterDescriptor> params,
            @JsonProperty("description") @Nullable String description,
            @JsonProperty("rule_builder_name") @Nullable String ruleBuilderName,
            @JsonProperty("rule_builder_title") @Nullable String ruleBuilderTitle,
            @JsonProperty("rule_builder_group") @Nullable RuleBuilderFunctionGroup ruleBuilderFunctionGroup) {
        return FunctionDescriptor.<T>builder()
                .name(name)
                .returnType(returnType)
                .params(params)
                .description(description)
                .ruleBuilderEnabled()
                .ruleBuilderName(ruleBuilderName)
                .ruleBuilderTitle(ruleBuilderTitle)
                .ruleBuilderFunctionGroup(ruleBuilderFunctionGroup)
                .build();
    }

}
