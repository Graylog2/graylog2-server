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
import org.graylog.events.fields.FieldValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatcherEvaluatorTest {
    @Mock
    private Event event;

    private MatcherEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new MatcherEvaluator();
    }

    @Test
    void fieldMatcherMatchesWhenEventFieldValueInValues() {
        when(event.getField("src_ip")).thenReturn(FieldValue.string("10.1.2.3"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.FIELD)
                .fieldName("src_ip")
                .values(ImmutableList.of("10.1.2.3", "10.1.2.4"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isTrue();
    }

    @Test
    void fieldMatcherDoesNotMatchWhenEventFieldValueOutsideValues() {
        when(event.getField("src_ip")).thenReturn(FieldValue.string("10.9.9.9"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.FIELD)
                .fieldName("src_ip")
                .values(ImmutableList.of("10.1.2.3", "10.1.2.4"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }

    @Test
    void fieldMatcherReturnsFalseWhenFieldMissing() {
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.FIELD)
                .fieldName("src_ip")
                .values(ImmutableList.of("10.1.2.3"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }

    @Test
    void fieldMatcherReturnsFalseWhenFieldIsError() {
        when(event.getField("src_ip")).thenReturn(FieldValue.error());
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.FIELD)
                .fieldName("src_ip")
                .values(ImmutableList.of("10.1.2.3"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }

    @Test
    void assetMatcherMatchesWhenAnyAssociatedAssetInValues() {
        when(event.getAssociatedAssets()).thenReturn(Set.of("asset-1", "asset-2"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.ASSET)
                .values(ImmutableList.of("asset-2", "asset-3"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isTrue();
    }

    @Test
    void assetMatcherDoesNotMatchWhenNoOverlap() {
        when(event.getAssociatedAssets()).thenReturn(Set.of("asset-1", "asset-2"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.ASSET)
                .values(ImmutableList.of("asset-3", "asset-4"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }

    @Test
    void assetMatcherReturnsFalseWhenNoAssociatedAssets() {
        when(event.getAssociatedAssets()).thenReturn(Set.of());
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.ASSET)
                .values(ImmutableList.of("asset-1"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }

    @Test
    void userMatcherMatchesAnyOfTheKnownUserFields() {
        when(event.getField("gl2_source_user")).thenReturn(FieldValue.string("alice"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.USER)
                .values(ImmutableList.of("alice"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isTrue();
    }

    @Test
    void userMatcherFallsBackThroughKnownFields() {
        lenient().when(event.getField("target_user")).thenReturn(FieldValue.string("alice"));
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.USER)
                .values(ImmutableList.of("alice"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isTrue();
    }

    @Test
    void userMatcherReturnsFalseWhenNoneOfTheUserFieldsMatch() {
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.USER)
                .values(ImmutableList.of("alice"))
                .build();

        assertThat(evaluator.matches(matcher, event)).isFalse();
    }
}
