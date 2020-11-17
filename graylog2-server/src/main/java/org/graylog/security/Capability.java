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
package org.graylog.security;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public enum Capability {
    @JsonProperty("view")
    VIEW(1),
    @JsonProperty("manage")
    MANAGE(2),
    @JsonProperty("own")
    OWN(3);

    private final int priority;

    public int priority() {
        return priority;
    }

    Capability(int priority) {
        this.priority = priority;
    }

    public String toId() {
        return name().toLowerCase(Locale.US);
    }
}
