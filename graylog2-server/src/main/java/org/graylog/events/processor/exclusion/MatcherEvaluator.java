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
package org.graylog.events.processor.exclusion;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Singleton;
import org.graylog.events.event.Event;
import org.graylog.events.fields.FieldValue;

import java.util.Set;

@Singleton
public class MatcherEvaluator {
    public boolean matches(Matcher matcher, Event event) {
        return switch (matcher.type()) {
            case FIELD -> fieldMatches(matcher.fieldName(), matcher.values(), event);
            case USER -> assetMatches(matcher.values(), event);
            case ASSET -> assetMatches(matcher.values(), event);
        };
    }

    private boolean fieldMatches(String fieldName, ImmutableList<String> values, Event event) {
        final FieldValue v = event.getField(fieldName);
        if (v == null || v.isError()) {
            return false;
        }
        return values.contains(v.value());
    }

    private boolean assetMatches(ImmutableList<String> values, Event event) {
        final Set<String> associated = event.getAssociatedAssets();
        if (associated == null || associated.isEmpty()) {
            return false;
        }
        return associated.stream().anyMatch(values::contains);
    }
}
