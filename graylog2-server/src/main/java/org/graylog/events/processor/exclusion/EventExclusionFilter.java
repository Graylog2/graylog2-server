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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.EventDefinition;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class EventExclusionFilter {
    private final MatcherEvaluator evaluator;

    @Inject
    public EventExclusionFilter(MatcherEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    public List<EventWithContext> filter(EventDefinition def, List<EventWithContext> events) {
        if (def.exclusions().isEmpty()) {
            return events;
        }
        final List<EventWithContext> notExcluded = new ArrayList<>(events.size());
        for (EventWithContext ewc : events) {
            final Event event = ewc.event();
            final String matchedRuleId = firstMatchingRuleId(def, event);
            if (matchedRuleId != null) {
                event.setExcludedByRuleId(matchedRuleId);
            } else {
                notExcluded.add(ewc);
            }
        }
        return notExcluded;
    }

    private String firstMatchingRuleId(EventDefinition def, Event event) {
        for (ExclusionRule rule : def.exclusions()) {
            boolean allMatch = true;
            for (Matcher matcher : rule.matchers()) {
                if (!evaluator.matches(matcher, event)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                return rule.id();
            }
        }
        return null;
    }
}
