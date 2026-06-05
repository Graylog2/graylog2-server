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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class TacticsTechniquesNormalizer {
    /**
     * Canonical MITRE ATT&amp;CK ID shapes accepted on event definitions: tactics (TA0000),
     * techniques (T0000), and sub-techniques (T0000.000).
     */
    public static final Pattern VALID_ID_PATTERN = Pattern.compile("^(TA\\d{4}|T\\d{4}(\\.\\d{3})?)$");

    private TacticsTechniquesNormalizer() {}

    public static ImmutableList<String> normalize(@Nullable Iterable<String> ids) {
        if (ids == null) {
            return ImmutableList.of();
        }
        return Streams.stream(ids)
                .filter(Objects::nonNull)
                .map(id -> id.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(ImmutableList.toImmutableList());
    }

    /** True if the ID (after normalization) is a canonical MITRE ATT&amp;CK shape. */
    public static boolean isValid(String id) {
        return id != null && VALID_ID_PATTERN.matcher(id).matches();
    }
}
