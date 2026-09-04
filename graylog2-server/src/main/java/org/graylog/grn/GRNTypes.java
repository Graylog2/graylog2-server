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

import com.google.common.collect.ImmutableSet;

public class GRNTypes {
    public static final GRNType BUILTIN_TEAM = GRNType.create("builtin-team");
    public static final GRNType DASHBOARD = GRNType.create("dashboard");
    public static final GRNType EVENT_DEFINITION = GRNType.create("event_definition");
    public static final GRNType EVENT_NOTIFICATION = GRNType.create("notification");
    public static final GRNType GRANT = GRNType.create("grant");
    public static final GRNType OUTPUT = GRNType.create("output");
    public static final GRNType ROLE = GRNType.create("role");
    public static final GRNType SEARCH = GRNType.create("search");
    public static final GRNType STREAM = GRNType.create("stream");
    public static final GRNType USER = GRNType.create("user");
    public static final GRNType SEARCH_FILTER = GRNType.create("search_filter");
    public static final GRNType FAVORITE = GRNType.create("favorite");
    public static final GRNType LAST_OPENED = GRNType.create("last_opened");
    public static final GRNType REPORT = GRNType.create("report");

    // TODO This is essentially the same as org.graylog2.contentpacks.model.ModelTypes
    // TODO find a way to unify these
    private static final ImmutableSet<GRNType> BUILTIN_TYPES = ImmutableSet.<GRNType>builder()
            .add(BUILTIN_TEAM)
            .add(DASHBOARD)
            .add(EVENT_DEFINITION)
            .add(EVENT_NOTIFICATION)
            .add(GRANT)
            .add(OUTPUT)
            .add(ROLE)
            .add(SEARCH)
            .add(STREAM)
            .add(USER)
            .add(SEARCH_FILTER)
            .add(FAVORITE)
            .add(LAST_OPENED)
            .add(REPORT)
            .build();

    /**
     * Returns the set of builtin GRN types.
     *
     * @return the builtin GRN types
     */
    public static ImmutableSet<GRNType> builtinTypes() {
        return BUILTIN_TYPES;
    }
}
