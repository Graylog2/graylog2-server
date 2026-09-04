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
package org.graylog.grn;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Represents a GRN type, which is a part of the {@link GRN}.
 */
@AutoValue
public abstract class GRNType {
    /**
     * Returns the type of the GRN, e.g. "stream", "dashboard", etc.
     *
     * @return the type of the GRN
     */
    public abstract String type();

    /**
     * Returns the GRN for the given entity ID
     *
     * @return the type of the GRN
     */
    public GRN toGRN(String entity) {
        return newGRNBuilder().entity(entity).build();
    }

    /**
     * Returns a new {@link GRN.Builder} for this GRN type.
     *
     * @return the builder for the GRN
     */
    public GRN.Builder newGRNBuilder() {
        return GRN.builder().type(type()).grnType(this);
    }

    /**
     * Creates a new GRNType instance with the specified type.
     *
     * @param type the type of the GRN, must not be null or empty
     * @return a new GRNType instance
     */
    public static GRNType create(String type) {
        checkArgument(!isNullOrEmpty(type), "type cannot be null or empty");

        return new AutoValue_GRNType(type);
    }
}
