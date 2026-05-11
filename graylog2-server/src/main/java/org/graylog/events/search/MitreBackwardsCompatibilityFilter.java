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

/**
 * Backwards-compatibility helper for the MITRE category filter during the migration window
 * where two event shapes coexist:
 *
 * <ul>
 *   <li>{@link #LEGACY_KEY} — Sigma-fired events store MITRE tags as numbered string fields
 *       at {@code event.fields.sigma_rule_tag_N} with values like {@code attack.t1059}.</li>
 *   <li>{@link #NEW_KEY} — Event-definition-fired events store MITRE IDs in a top-level
 *       keyword array {@code event.mitre_categories} with values like {@code T1059}.</li>
 * </ul>
 *
 * Storage adapters call {@link #emitShouldClauses} once per search; the consumer wraps the
 * emitted clauses in a bool filter with {@code minimum_should_match=1} so a doc only needs
 * to match one shape. Other extraFilter keys still AND across, so callers use
 * {@link #isMitreKey} to skip these two in the generic loop.
 *
 * <p>Slated for removal in 8.0 once Sigma-fired events stop writing the legacy shape.
 */
public final class MitreBackwardsCompatibilityFilter {
    public static final String LEGACY_KEY = EventDto.FIELD_FIELDS + ".sigma_rule_tag_*";
    public static final String NEW_KEY = EventDto.FIELD_MITRE_CATEGORIES;

    private MitreBackwardsCompatibilityFilter() {}

    /**
     * Invokes {@code shouldClause} for every MITRE value (both shapes) that should be OR'd
     * together in the search filter. Returns {@code true} if any value was emitted —
     * callers should then attach the accumulated should-clauses as a single filter clause
     * with {@code minimum_should_match=1}.
     */
    public static boolean emitShouldClauses(Map<String, Set<String>> extraFilters,
                                            BiConsumer<String, String> shouldClause) {
        final Set<String> legacy = extraFilters.getOrDefault(LEGACY_KEY, Set.of());
        final Set<String> newShape = extraFilters.getOrDefault(NEW_KEY, Set.of());
        legacy.forEach(v -> shouldClause.accept(LEGACY_KEY, v));
        newShape.forEach(v -> shouldClause.accept(NEW_KEY, v));
        return !legacy.isEmpty() || !newShape.isEmpty();
    }

    /**
     * Returns {@code true} if the given {@code extraFilters} key is one of the two MITRE
     * shapes handled by {@link #emitShouldClauses}. Callers use this to skip those keys in
     * their generic extraFilter loop (otherwise the keys would AND with the OR'd predicate
     * already added).
     */
    public static boolean isMitreKey(String key) {
        return LEGACY_KEY.equals(key) || NEW_KEY.equals(key);
    }
}
