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
package org.graylog2.indexer.searches;

import org.graylog2.plugin.Message;

import java.util.Locale;

public class Sorting {

    public static final Sorting DEFAULT = new Sorting(Message.FIELD_TIMESTAMP, Direction.DESC);

    public enum Direction {
        ASC,
        DESC
    }

    private final String field;
    private final Direction direction;

    public Sorting(String field, Direction direction) {
        this.field = field;
        this.direction = direction;
    }

    public String getField() {
        return field;
    }

    public Direction getDirection() { return this.direction; }

    public static Sorting fromApiParam(String param) {
        if (param == null || !param.contains(":")) {
            throw new IllegalArgumentException("Invalid sorting parameter: " + param);
        }

        String[] parts = param.split(":");

        return new Sorting(parts[0], Direction.valueOf(parts[1].toUpperCase(Locale.ENGLISH)));
    }

}
