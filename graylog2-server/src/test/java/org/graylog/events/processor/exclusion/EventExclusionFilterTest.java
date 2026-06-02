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
import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.graylog.events.processor.EventDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventExclusionFilterTest {
    private MatcherEvaluator evaluator;
    private EventExclusionFilter filter;

    @BeforeEach
    void setUp() {
        evaluator = mock(MatcherEvaluator.class);
        filter = new EventExclusionFilter(evaluator);
    }

    @Test
    void returnsAllEventsWhenNoExclusionsConfigured() {
        final EventDefinition def = mock(EventDefinition.class);
        when(def.exclusions()).thenReturn(ImmutableList.of());
        final EventWithContext e1 = withContext(mock(Event.class));
        final EventWithContext e2 = withContext(mock(Event.class));

        final List<EventWithContext> result = filter.filter(def, List.of(e1, e2));

        assertThat(result).containsExactly(e1, e2);
    }

    @Test
    void filtersOutMatchingEventsAndTagsThem() {
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.USER).values(ImmutableList.of("alice")).build();
        final ExclusionRule rule = ExclusionRule.builder()
                .id("rule-1").title("t")
                .matchers(ImmutableList.of(matcher))
                .build();
        final EventDefinition def = mock(EventDefinition.class);
        when(def.exclusions()).thenReturn(ImmutableList.of(rule));

        final Event matching = mock(Event.class);
        final Event nonMatching = mock(Event.class);
        when(evaluator.matches(matcher, matching)).thenReturn(true);
        when(evaluator.matches(matcher, nonMatching)).thenReturn(false);

        final EventWithContext eM = withContext(matching);
        final EventWithContext eN = withContext(nonMatching);

        final List<EventWithContext> result = filter.filter(def, List.of(eM, eN));

        assertThat(result).containsExactly(eN);
        verify(matching).setExcludedByRuleId("rule-1");
        verify(nonMatching, never()).setExcludedByRuleId(eq("rule-1"));
    }

    @Test
    void ruleMatchesOnlyWhenAllMatchersMatch() {
        final Matcher m1 = Matcher.builder().type(MatcherType.USER).values(ImmutableList.of("a")).build();
        final Matcher m2 = Matcher.builder().type(MatcherType.ASSET).values(ImmutableList.of("x")).build();
        final ExclusionRule rule = ExclusionRule.builder()
                .id("r").title("t").matchers(ImmutableList.of(m1, m2)).build();
        final EventDefinition def = mock(EventDefinition.class);
        when(def.exclusions()).thenReturn(ImmutableList.of(rule));

        final Event event = mock(Event.class);
        when(evaluator.matches(m1, event)).thenReturn(true);
        when(evaluator.matches(m2, event)).thenReturn(false);

        final EventWithContext e = withContext(event);
        final List<EventWithContext> result = filter.filter(def, List.of(e));

        assertThat(result).containsExactly(e);
        verify(event, never()).setExcludedByRuleId(eq("r"));
    }

    @Test
    void recordsFirstMatchingRuleByDeclarationOrder() {
        final Matcher mA = Matcher.builder().type(MatcherType.USER).values(ImmutableList.of("a")).build();
        final Matcher mB = Matcher.builder().type(MatcherType.USER).values(ImmutableList.of("b")).build();
        final ExclusionRule r1 = ExclusionRule.builder().id("first").title("t").matchers(ImmutableList.of(mA)).build();
        final ExclusionRule r2 = ExclusionRule.builder().id("second").title("t").matchers(ImmutableList.of(mB)).build();
        final EventDefinition def = mock(EventDefinition.class);
        when(def.exclusions()).thenReturn(ImmutableList.of(r1, r2));

        final Event event = mock(Event.class);
        when(evaluator.matches(mA, event)).thenReturn(true);
        when(evaluator.matches(mB, event)).thenReturn(true);

        filter.filter(def, List.of(withContext(event)));

        verify(event).setExcludedByRuleId("first");
        verify(event, never()).setExcludedByRuleId(eq("second"));
    }

    @Test
    void integratesWithRealMatcherEvaluator() {
        final EventExclusionFilter realFilter = new EventExclusionFilter(new MatcherEvaluator());

        final ExclusionRule rule = ExclusionRule.builder()
                .id("scanner-rule").title("Suppress scanner traffic")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER)
                        .values(ImmutableList.of("alice"))
                        .build()))
                .build();
        final EventDefinition def = mock(EventDefinition.class);
        when(def.exclusions()).thenReturn(ImmutableList.of(rule));

        final Event aliceEvent = mock(Event.class);
        final Event bobEvent = mock(Event.class);
        when(aliceEvent.getField("gl2_source_user")).thenReturn(FieldValue.string("alice"));
        when(bobEvent.getField("gl2_source_user")).thenReturn(FieldValue.string("bob"));

        final List<EventWithContext> result = realFilter.filter(def,
                List.of(withContext(aliceEvent), withContext(bobEvent)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).event()).isSameAs(bobEvent);
        verify(aliceEvent).setExcludedByRuleId("scanner-rule");
        verify(bobEvent, never()).setExcludedByRuleId(anyString());
    }

    private static EventWithContext withContext(Event event) {
        final EventWithContext ewc = mock(EventWithContext.class);
        when(ewc.event()).thenReturn(event);
        return ewc;
    }
}
