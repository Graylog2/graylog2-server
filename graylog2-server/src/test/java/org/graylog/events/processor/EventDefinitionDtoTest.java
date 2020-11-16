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
import com.google.common.collect.ImmutableMap;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog2.plugin.rest.ValidationResult;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventDefinitionDtoTest {
    private EventDefinitionDto testSubject;

    @Before
    public void setUp() throws Exception {
        final AggregationEventProcessorConfig configMock = mock(AggregationEventProcessorConfig.class);
        when(configMock.validate()).thenReturn(new ValidationResult());

        testSubject = EventDefinitionDto.builder()
            .title("foo")
            .description("bar")
            .priority(1)
            .alert(false)
            .config(configMock)
            .keySpec(ImmutableList.<String>builder().build())
            .notificationSettings(EventNotificationSettings.withGracePeriod(0))
            .build();
    }

    @Test
    public void testValidateWithEmptyTitle() {
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .title("")
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("title");
    }

    @Test
    public void testValidateWithEmptyConfigType() {
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .config(new EventProcessorConfig.FallbackConfig())
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("config");
    }

    @Test
    public void testValidateWithInvalidConfig() {
        final AggregationEventProcessorConfig configMock = mock(AggregationEventProcessorConfig.class);
        final ValidationResult mockedValidationResult = new ValidationResult();
        mockedValidationResult.addError("foo", "bar");
        when(configMock.validate()).thenReturn(mockedValidationResult);

        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .config(configMock)
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("foo");
    }

    @Test
    public void testValidateWithInvalidFieldName() {
        final EventFieldSpec fieldSpecMock = mock(EventFieldSpec.class);
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .fieldSpec(ImmutableMap.of("foo\\bar", fieldSpecMock, "$yo&^a", fieldSpecMock))
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("field_spec");
        final List<String> fieldValidation = (List<String>) validationResult.getErrors().get("field_spec");
        assertThat(fieldValidation.size()).isEqualTo(2);
        assertThat(fieldValidation.get(0)).contains("foo\\bar");
        assertThat(fieldValidation.get(1)).contains("$yo&^a");
    }

    @Test
    public void testValidateWithKeySpecNotInFieldSpec() {
        final EventFieldSpec fieldSpecMock = mock(EventFieldSpec.class);
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .fieldSpec(ImmutableMap.of("bar", fieldSpecMock, "baz", fieldSpecMock))
            .keySpec(ImmutableList.of("foo"))
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("key_spec");
    }

    @Test
    public void testValidEventDefinition() {
        final ValidationResult validationResult = testSubject.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testValidEventDefinitionWithKeySpecInFieldSpec() {
        final EventFieldSpec fieldSpecMock = mock(EventFieldSpec.class);
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .fieldSpec(ImmutableMap.of("foo", fieldSpecMock, "bar", fieldSpecMock))
            .keySpec(ImmutableList.of("foo", "bar"))
            .build();
        final ValidationResult validationResult = invalidEventDefinition.validate();
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }
}
