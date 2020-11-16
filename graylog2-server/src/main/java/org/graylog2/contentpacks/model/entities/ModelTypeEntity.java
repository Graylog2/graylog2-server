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
package org.graylog2.contentpacks.model.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

@AutoValue
public abstract class ModelTypeEntity {
    @JsonValue
    public abstract ValueReference type();

    @Override
    public String toString() {
        return type().asString();
    }

    @JsonCreator
    public static ModelTypeEntity of(ValueReference type) {
        Preconditions.checkArgument(StringUtils.isNotBlank(type.asString()), "Type must not be blank");
        return new AutoValue_ModelTypeEntity(type);
    }

    public static ModelTypeEntity of(String type) {
        return of(ValueReference.of(type));
    }
}
