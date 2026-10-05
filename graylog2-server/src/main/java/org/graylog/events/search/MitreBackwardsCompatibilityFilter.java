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
package org.graylog.events.search;

import org.graylog.events.event.EventDto;

import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/** OR's the legacy sigma_rule_tag_* and new tactics_techniques filter shapes. Dies in 8.0. */
public final class MitreBackwardsCompatibilityFilter {
    public static final String LEGACY_KEY = EventDto.FIELD_FIELDS + ".sigma_rule_tag_*";
    public static final String NEW_KEY = EventDto.FIELD_TACTICS_TECHNIQUES;

    private MitreBackwardsCompatibilityFilter() {}

    public static boolean emitShouldClauses(Map<String, Set<String>> extraFilters,
                                            BiConsumer<String, String> shouldClause) {
        final Set<String> legacy = extraFilters.getOrDefault(LEGACY_KEY, Set.of());
        final Set<String> newShape = extraFilters.getOrDefault(NEW_KEY, Set.of());
        legacy.forEach(v -> shouldClause.accept(LEGACY_KEY, v));
        newShape.forEach(v -> shouldClause.accept(NEW_KEY, v));
        return !legacy.isEmpty() || !newShape.isEmpty();
    }

    public static boolean isMitreKey(String key) {
        return LEGACY_KEY.equals(key) || NEW_KEY.equals(key);
    }
}
