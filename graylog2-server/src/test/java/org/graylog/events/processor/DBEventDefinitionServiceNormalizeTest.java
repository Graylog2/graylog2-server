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
import com.google.common.collect.ImmutableSet;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.exclusion.ExclusionRule;
import org.graylog.events.processor.exclusion.Matcher;
import org.graylog.events.processor.exclusion.MatcherType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DBEventDefinitionServiceNormalizeTest {

    @Test
    void returnsSameDtoWhenExclusionsListIsEmpty() {
        final EventDefinitionDto dto = sampleBuilder().exclusions(ImmutableList.of()).build();
        final EventDefinitionDto normalized = DBEventDefinitionService.normalizeExclusionIds(dto);
        assertThat(normalized).isSameAs(dto);
    }

    @Test
    void mintsIdForExclusionWithNullId() {
        final ExclusionRule rule = ExclusionRule.builder()
                .title("scanner")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER).values(ImmutableList.of("alice")).build()))
                .build();
        final EventDefinitionDto dto = sampleBuilder().exclusions(ImmutableList.of(rule)).build();

        final EventDefinitionDto normalized = DBEventDefinitionService.normalizeExclusionIds(dto);

        assertThat(normalized.exclusions()).hasSize(1);
        assertThat(normalized.exclusions().get(0).id()).isNotNull().isNotBlank();
    }

    @Test
    void mintsIdForExclusionWithBlankId() {
        final ExclusionRule rule = ExclusionRule.builder()
                .id("   ")
                .title("scanner")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER).values(ImmutableList.of("alice")).build()))
                .build();
        final EventDefinitionDto dto = sampleBuilder().exclusions(ImmutableList.of(rule)).build();

        final EventDefinitionDto normalized = DBEventDefinitionService.normalizeExclusionIds(dto);

        final String id = normalized.exclusions().get(0).id();
        assertThat(id).isNotNull();
        assertThat(id.trim()).isNotEmpty();
        assertThat(id).isNotEqualTo("   ");
    }

    @Test
    void preservesExistingExclusionIds() {
        final ExclusionRule rule = ExclusionRule.builder()
                .id("user-supplied-id")
                .title("scanner")
                .matchers(ImmutableList.of(Matcher.builder()
                        .type(MatcherType.USER).values(ImmutableList.of("alice")).build()))
                .build();
        final EventDefinitionDto dto = sampleBuilder().exclusions(ImmutableList.of(rule)).build();

        final EventDefinitionDto normalized = DBEventDefinitionService.normalizeExclusionIds(dto);

        assertThat(normalized.exclusions().get(0).id()).isEqualTo("user-supplied-id");
    }

    private EventDefinitionDto.Builder sampleBuilder() {
        return EventDefinitionDto.builder()
                .title("test")
                .description("test")
                .priority(1)
                .alert(false)
                .keySpec(ImmutableList.of())
                .config(TestEventProcessorConfig.builder()
                        .message("test")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .notificationSettings(EventNotificationSettings.withGracePeriod(0))
                .tags(ImmutableSet.of());
    }
}
