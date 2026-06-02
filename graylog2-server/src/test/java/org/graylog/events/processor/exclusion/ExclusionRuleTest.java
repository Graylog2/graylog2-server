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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExclusionRuleTest {
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapperProvider().get();
    }

    @Test
    void serializesAndDeserializesRoundTrip() throws Exception {
        final ExclusionRule rule = ExclusionRule.builder()
                .id("rule-1")
                .title("Suppress scanner traffic")
                .matchers(ImmutableList.of(
                        Matcher.builder()
                                .type(MatcherType.USER)
                                .values(ImmutableList.of("scanner-bot", "qa-runner"))
                                .build(),
                        Matcher.builder()
                                .type(MatcherType.FIELD)
                                .fieldName("src_subnet")
                                .values(ImmutableList.of("10.0.0.0/24"))
                                .build()))
                .build();

        final String json = objectMapper.writeValueAsString(rule);
        final ExclusionRule roundTrip = objectMapper.readValue(json, ExclusionRule.class);
        assertThat(roundTrip).isEqualTo(rule);
    }

    @Test
    void rejectsRuleWithNoMatchers() {
        assertThatThrownBy(() -> ExclusionRule.builder()
                .id("r")
                .title("t")
                .matchers(ImmutableList.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one matcher");
    }

    @Test
    void rejectsMatcherWithEmptyValues() {
        assertThatThrownBy(() -> Matcher.builder()
                .type(MatcherType.ASSET)
                .values(ImmutableList.of())
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one value");
    }

    @Test
    void rejectsFieldMatcherWithBlankFieldName() {
        assertThatThrownBy(() -> Matcher.builder()
                .type(MatcherType.FIELD)
                .fieldName("  ")
                .values(ImmutableList.of("v"))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fieldName");
    }

    @Test
    void allowsNonFieldMatchersWithoutFieldName() {
        final Matcher matcher = Matcher.builder()
                .type(MatcherType.USER)
                .values(ImmutableList.of("scanner-bot"))
                .build();
        assertThat(matcher.fieldName()).isNull();
        assertThat(matcher.type()).isEqualTo(MatcherType.USER);
    }

    @Test
    void toBuilderProducesEqualRuleAndAllowsMutation() {
        final ExclusionRule original = ExclusionRule.builder()
                .id("original-id")
                .title("Original")
                .matchers(ImmutableList.of(
                        Matcher.builder()
                                .type(MatcherType.USER)
                                .values(ImmutableList.of("scanner-bot"))
                                .build()))
                .build();

        final ExclusionRule mutated = original.toBuilder().id("new-id").build();

        assertThat(mutated.id()).isEqualTo("new-id");
        assertThat(original.id()).isEqualTo("original-id");
    }

    @Test
    void rejectsRuleWithBlankId() {
        assertThatThrownBy(() -> ExclusionRule.builder()
                .id("  ")
                .matchers(ImmutableList.of(
                        Matcher.builder()
                                .type(MatcherType.USER)
                                .values(ImmutableList.of("scanner-bot"))
                                .build()))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rejectsRuleWithNullId() {
        assertThatThrownBy(() -> ExclusionRule.builder()
                .matchers(ImmutableList.of(
                        Matcher.builder()
                                .type(MatcherType.USER)
                                .values(ImmutableList.of("scanner-bot"))
                                .build()))
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }
}
