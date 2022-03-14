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

import java.util.Optional;
import java.util.Set;

/**
 * View Resolvers provide a way that plugins can provide custom sources for looking up views.
 */
public interface ViewResolver {
    Optional<ViewDTO> get(String id);

    /**
     * @return A set of all search ids referenced by resolvable views. These must be returned to prevent the searches
     * from being automatically deleted by periodic Graylog search maintenance.
     */
    Set<String> getSearchIds();
}
