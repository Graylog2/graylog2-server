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
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class ParameterDescriptor<T, R> {

    @JsonProperty
    public abstract Class<? extends T> type();

    @JsonProperty
    public abstract Class<? extends R> transformedType();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract boolean optional();

    @JsonProperty
    public abstract Optional<T> defaultValue();

    @JsonProperty
    public abstract boolean ruleBuilderVariable();

    @JsonProperty
    public abstract boolean allowNegatives();

    @JsonIgnore
    public abstract java.util.function.Function<T, R> transform();

    @JsonProperty
    @Nullable
    public abstract String description();

    public static <T, R> Builder<T, R> param() {
        return ParameterDescriptor.<T, R>builder().optional(false).defaultValue(Optional.empty()).ruleBuilderVariable(false).allowNegatives(false);
    }

    public static Builder<String, String> string(String name) {
        return string(name, String.class);
    }

    public static <R> Builder<String, R> string(String name, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<String, R>param().type(String.class).transformedType(transformedClass).name(name);
    }

    public static Builder<Object, Object> object(String name) {
        return object(name, Object.class);
    }

    public static <R> Builder<Object, R> object(String name, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<Object, R>param().type(Object.class).transformedType(transformedClass).name(name);
    }

    public static Builder<Long, Long> integer(String name) {
        return integer(name, Long.class);
    }

    public static <R> Builder<Long, R> integer(String name, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<Long, R>param().type(Long.class).transformedType(transformedClass).name(name);
    }

    public static Builder<Double, Double> floating(String name) {
        return floating(name, Double.class);
    }

    public static <R> Builder<Double, R> floating(String name, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<Double, R>param().type(Double.class).transformedType(transformedClass).name(name);
    }

    public static Builder<Boolean, Boolean> bool(String name) {
        return bool(name, Boolean.class);
    }

    public static <R> Builder<Boolean, R> bool(String name, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<Boolean, R>param().type(Boolean.class).transformedType(transformedClass).name(name);
    }

    public static <T> Builder<T, T> type(String name, Class<? extends T> typeClass) {
        return type(name, typeClass, typeClass);
    }

    public static <T, R> Builder<T, R> type(String name, Class<? extends T> typeClass, Class<? extends R> transformedClass) {
        return ParameterDescriptor.<T, R>param().type(typeClass).transformedType(transformedClass).name(name);
    }

    @Nullable
    public R required(FunctionArgs args, EvaluationContext context) {
        final Object precomputedValue = args.getPreComputedValue(name());
        if (precomputedValue != null) {
            return transformedType().cast(precomputedValue);
        }
        final Expression valueExpr = args.expression(name());
        if (valueExpr == null) {
            return null;
        }
        final Object value = valueExpr.evaluateUnsafe(context);
        return transformedType().cast(transform().apply(type().cast(value)));
    }

    public Optional<R> optional(FunctionArgs args, EvaluationContext context) {
        return Optional.ofNullable(required(args, context));
    }

    public static <T, R> ParameterDescriptor.Builder<T, R> builder() {
        //noinspection unchecked
        return new AutoValue_ParameterDescriptor.Builder<T, R>()
                .transform((java.util.function.Function<T, R>) java.util.function.Function.<T>identity());
    }

    @AutoValue.Builder()
    public static abstract class Builder<T, R> {
        public abstract Builder<T, R> type(Class<? extends T> type);

        public abstract Builder<T, R> transformedType(Class<? extends R> type);

        public abstract Builder<T, R> name(String name);

        public abstract Builder<T, R> optional(boolean optional);

        public abstract Builder<T, R> defaultValue(Optional<T> defaultValue);

        public abstract Builder<T, R> ruleBuilderVariable(boolean ruleBuilderVariable);

        public abstract Builder<T, R> allowNegatives(boolean allowNegatives);

        public Builder<T, R> optional() {
            return optional(true);
        }

        public Builder<T, R> ruleBuilderVariable() {
            return ruleBuilderVariable(true);
        }

        public abstract Builder<T, R> description(String description);

        public abstract ParameterDescriptor<T, R> build();

        public abstract Builder<T, R> transform(java.util.function.Function<T, R> transform);
    }

    @JsonCreator
    public static <T, R> ParameterDescriptor<T, R> createForRuleBuilder(
            @JsonProperty("type") Class<? extends T> type,
            @JsonProperty("transformed_type") Class<? extends R> transformedType,
            @JsonProperty("name") String name,
            @JsonProperty("description") @Nullable String description) {
        return ParameterDescriptor.<T, R>param()
                .type(type)
                .transformedType(transformedType)
                .name(name)
                .description(description)
                .build();
    }
}
