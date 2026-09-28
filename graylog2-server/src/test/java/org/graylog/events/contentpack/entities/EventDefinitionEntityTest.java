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
package org.graylog.events.contentpack.entities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.fields.FieldValueType;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventDefinitionEntityTest {

    @Test
    void sortsFieldSpecAlphabetically() {
        final EventFieldSpec spec = EventFieldSpec.builder()
                .dataType(FieldValueType.STRING)
                .providers(ImmutableList.of())
                .build();

        final ImmutableMap<String, EventFieldSpec> sorted =
                buildEntityWithFieldSpec(ImmutableMap.of("zulu", spec, "alpha", spec)).fieldSpec();

        assertThat(sorted.keySet()).containsExactly("alpha", "zulu");
    }

    private static EventDefinitionEntity buildEntityWithFieldSpec(ImmutableMap<String, EventFieldSpec> fieldSpec) {
        return EventDefinitionEntity.builder()
                .title(ValueReference.of("title"))
                .description(ValueReference.of("description"))
                .priority(ValueReference.of(1))
                .config(mock(EventProcessorConfigEntity.class))
                .alert(ValueReference.of(false))
                .fieldSpec(fieldSpec)
                .keySpec(ImmutableList.of())
                .notificationSettings(EventNotificationSettings.builder()
                        .gracePeriodMs(60000)
                        .backlogSize(0)
                        .build())
                .notifications(ImmutableList.of())
                .storage(ImmutableList.of())
                .isScheduled(ValueReference.of(true))
                .build();
    }
}
