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
package org.graylog2.contentpacks.model.entities.references;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.graylog2.contentpacks.jackson.ValueTypeDeserializer;
import org.graylog2.contentpacks.jackson.ValueTypeSerializer;

@JsonSerialize(using = ValueTypeSerializer.class)
@JsonDeserialize(using = ValueTypeDeserializer.class)
public enum ValueType {
    BOOLEAN(Boolean.class),
    DOUBLE(Double.class),
    FLOAT(Float.class),
    INTEGER(Integer.class),
    LONG(Long.class),
    STRING(String.class),
    PARAMETER(Void.class);

    private final Class<?> targetClass;

    ValueType(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    public Class<?> targetClass() {
        return targetClass;
    }
}