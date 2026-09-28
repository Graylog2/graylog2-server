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
package org.graylog.events.fields;

import com.google.common.collect.ImmutableSortedMap;

import java.util.Comparator;
import java.util.Map;

/**
 * Ordering for event field names. Case-insensitive so that the order matches the web
 * interface, with a natural-order tiebreak so the comparator stays consistent with
 * equals - without it, field names differing only in case would compare equal and
 * {@link ImmutableSortedMap#copyOf(Map, Comparator)} would reject the map.
 */
public final class EventFieldNames {
    public static final Comparator<String> COMPARATOR =
            String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder());

    private EventFieldNames() {
    }

    public static <V> ImmutableSortedMap<String, V> sorted(Map<String, V> fields) {
        return ImmutableSortedMap.copyOf(fields, COMPARATOR);
    }
}
