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
package org.graylog2.entitygroups.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

public enum EntityType {
    @JsonProperty("assets")
    ASSETS,
    @JsonProperty("sigma_rules")
    SIGMA_RULES;

    public String getName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static EntityType valueOfIgnoreCase(String name) {
        return EntityType.valueOf(name.toUpperCase(Locale.ROOT));
    }
}
