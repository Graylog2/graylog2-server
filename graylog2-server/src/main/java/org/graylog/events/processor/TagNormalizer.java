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
import java.util.regex.Pattern;

public final class TagNormalizer {
    /**
     * Allowed character set for an event-definition tag. Lowercase ASCII letters, digits, hyphen,
     * underscore, and dot. Restricting to this set keeps tag values safe to embed directly in
     * search queries (no Lucene/Mongo metacharacters) and avoids surprises in URL filters.
     */
    public static final Pattern VALID_TAG_PATTERN = Pattern.compile("[a-z0-9_.-]+");

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

    /** True if the tag (after normalization) contains only allowed characters. */
    public static boolean isValid(String tag) {
        return tag != null && VALID_TAG_PATTERN.matcher(tag).matches();
    }
}
