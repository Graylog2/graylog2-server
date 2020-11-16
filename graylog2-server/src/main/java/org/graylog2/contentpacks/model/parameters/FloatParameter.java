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
package org.graylog2.contentpacks.model.parameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ValueType;

@AutoValue
@JsonDeserialize(builder = AutoValue_FloatParameter.Builder.class)
public abstract class FloatParameter implements Parameter<Float> {
    static final String TYPE_NAME = "float";

    public static Builder builder() {
        return new AutoValue_FloatParameter.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder implements ParameterBuilder<Builder, Float> {
        abstract FloatParameter autoBuild();

        public FloatParameter build() {
            valueType(ValueType.FLOAT);
            return autoBuild();
        }
    }
}
