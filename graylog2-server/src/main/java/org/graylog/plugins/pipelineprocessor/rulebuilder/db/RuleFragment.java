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
package org.graylog.plugins.pipelineprocessor.rulebuilder.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.Objects;

@AutoValue
@JsonIgnoreProperties(value = {"name"}, allowGetters = true)
public abstract class RuleFragment {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_FRAGMENT = "fragment";
    public static final String FIELD_FRAGMENT_OUTPUT = "fragment_output_variable";
    public static final String FIELD_CONDITION = "isCondition";
    public static final String FIELD_DESCRIPTOR = "descriptor";

    @JsonIgnore
    @Nullable
    @Id
    @ObjectId
    public abstract String id();

    @JsonProperty(FIELD_NAME)
    public String getName() {
        return descriptor().name();
    }

    @JsonProperty(FIELD_FRAGMENT)
    @Nullable
    public abstract String fragment();

    @JsonProperty(FIELD_FRAGMENT_OUTPUT)
    @Nullable
    public abstract String fragmentOutputVariable();

    @JsonProperty(FIELD_CONDITION)
    public abstract boolean isCondition();

    @JsonProperty(FIELD_DESCRIPTOR)
    public abstract FunctionDescriptor descriptor();


    public static Builder builder() {
        return new AutoValue_RuleFragment.Builder().isCondition(false);
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder id(String id);

        public abstract Builder descriptor(FunctionDescriptor descriptor);

        public abstract Builder fragment(String fragment);

        public abstract Builder isCondition(boolean isCondition);

        public Builder isCondition() {
            return isCondition(true);
        }

        public abstract Builder fragmentOutputVariable(String fragmentOutputVariable);

        public abstract RuleFragment build();

    }

    @JsonIgnore
    public boolean isFragment() {
        return Objects.nonNull(fragment());
    }

    @JsonIgnore
    public boolean isFunction() {
        return Objects.isNull(fragment());
    }

    @JsonCreator
    public static RuleFragment create(
            @Id @ObjectId @JsonProperty("_id") @Nullable String id,
            @JsonProperty(FIELD_FRAGMENT) String fragment,
            @JsonProperty(FIELD_FRAGMENT_OUTPUT) @Nullable String fragmentOutputVariable,
            @JsonProperty(FIELD_CONDITION) boolean isCondition,
            @JsonProperty(FIELD_DESCRIPTOR) FunctionDescriptor descriptor) {
        return builder()
                .id(id)
                .fragment(fragment)
                .fragmentOutputVariable(fragmentOutputVariable)
                .isCondition(isCondition)
                .descriptor(descriptor)
                .build();
    }

}
