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
    public static final GRNType BUILTIN_TEAM = GRNType.create("builtin-team", "XXX-NOT-A-REAL-TYPE-XXX:");
    public static final GRNType COLLECTION = GRNType.create("collection", "collections:");
    public static final GRNType DASHBOARD = GRNType.create("dashboard", "dashboards:");
    public static final GRNType EVENT_DEFINITION = GRNType.create("event_definition", "eventdefinitions:");
    public static final GRNType EVENT_NOTIFICATION = GRNType.create("notification", "eventnotifications:");
    public static final GRNType GRANT = GRNType.create("grant", "grants:");
    public static final GRNType ROLE = GRNType.create("role", "roles:");
    public static final GRNType SEARCH = GRNType.create("search", "view:");
    public static final GRNType STREAM = GRNType.create("stream", "streams:");
    public static final GRNType TEAM = GRNType.create("team", "teams:");
    public static final GRNType USER = GRNType.create("user", "users:");

    // TODO This is essentially the same as org.graylog2.contentpacks.model.ModelTypes
    // TODO find a way to unify these
    private static final ImmutableSet<GRNType> BUILTIN_TYPES = ImmutableSet.<GRNType>builder()
            .add(BUILTIN_TEAM)
            .add(COLLECTION)
            .add(DASHBOARD)
            .add(EVENT_DEFINITION)
            .add(EVENT_NOTIFICATION)
            .add(GRANT)
            .add(ROLE)
            .add(SEARCH)
            .add(STREAM)
            .add(TEAM)
            .add(USER)
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
