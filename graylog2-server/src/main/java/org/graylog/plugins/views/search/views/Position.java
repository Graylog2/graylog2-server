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
package org.graylog.plugins.views.search.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

public class Position {
    private static Position infinity() {
        return new Infinity();
    }

    @JsonCreator
    public static Position fromInt(int value) {
        return new NumberPosition(value);
    }

    @JsonCreator
    public static Position fromDouble(Double value) {
        if (value.equals(Infinity.value)) {
            return infinity();
        }
        else {
            return new NumberPosition(value.intValue());
        }
    }

    @JsonCreator
    public static Position fromString(String value) {
        if (value.equals("Infinity")) {
            return infinity();
        }
        else {
            throw new IllegalArgumentException("Unable to deserialize " + value + " to Position.");
        }
    }
}

class Infinity extends Position {
    static final Double value = Double.POSITIVE_INFINITY;

    Infinity() {
        super();
    }

    @JsonValue
    public Double jsonValue() {
        return value;
    }
}

class NumberPosition extends Position {
    private final int value;

    NumberPosition(int value) {
        super();
        this.value = value;
    }

    @JsonValue
    public int jsonValue() {
        return this.value;
    }
}
