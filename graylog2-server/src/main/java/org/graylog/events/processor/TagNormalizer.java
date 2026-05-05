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
package org.graylog.events.processor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;

public final class TagNormalizer {
    private TagNormalizer() {}

    public static ImmutableSet<String> normalize(@Nullable Iterable<String> tags) {
        if (tags == null) {
            return ImmutableSet.of();
        }
        return Streams.stream(tags)
                .filter(Objects::nonNull)
                .map(tag -> tag.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(ImmutableSet.toImmutableSet());
    }
}
