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
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.notifications.EventNotificationSettings;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDefinitionNormalizersTest {
    private final EventDefinitionDto dto = EventDefinitionDto.builder()
            .title("Test")
            .description("Test")
            .priority(1)
            .config(TestEventProcessorConfig.builder()
                    .message("test")
                    .searchWithinMs(60000)
                    .executeEveryMs(60000)
                    .build())
            .keySpec(ImmutableList.of())
            .alert(false)
            .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
            .build();

    @Test
    void noneReturnsTheDefinitionUnchanged() {
        assertThat(EventDefinitionNormalizers.none().normalize(dto)).isSameAs(dto);
    }

    @Test
    void appliesEachNormalizerToTheResultOfTheLast() {
        final EventDefinitionNormalizer appendA = d -> d.toBuilder().title(d.title() + "-a").build();
        final EventDefinitionNormalizer appendB = d -> d.toBuilder().title(d.title() + "-b").build();
        final var normalizers = new EventDefinitionNormalizers(new LinkedHashSet<>(List.of(appendA, appendB)));

        assertThat(normalizers.normalize(dto).title()).isEqualTo("Test-a-b");
    }
}
