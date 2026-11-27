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
package org.graylog.plugins.views.search;

import com.google.errorprone.annotations.MustBeClosed;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Maps stream categories to the IDs of the streams that have one of the given categories.
 */
@FunctionalInterface
public interface StreamCategoryMapper {
    /**
     * @param categories the stream categories to map
     * @return a {@code java.util.stream.Stream} of the IDs of the streams with one of the given categories. Implementations may return a
     * stream backed by a database cursor, so it must be closed by the caller.
     */
    @MustBeClosed
    Stream<String> apply(Collection<String> categories);
}
