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
package org.graylog.plugins.views.migrations;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

class LegacyViewsPermissions {
    static final String VIEW_USE = "view:use";
    static final String VIEW_CREATE = "view:create";
    static final String EXTENDEDSEARCH_CREATE = "extendedsearch:create";
    static final String EXTENDEDSEARCH_USE = "extendedsearch:use";

    static Set<String> all() {
        return ImmutableSet.of(
                VIEW_USE,
                VIEW_CREATE,
                EXTENDEDSEARCH_USE,
                EXTENDEDSEARCH_CREATE);
    }
}
