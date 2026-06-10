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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.TestEventProcessorConfig;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.aggregation.AggregationEventProcessorConfig;
import org.graylog.security.UserContext;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EventDefinitionDtoTest {
    private EventDefinitionDto testSubject;

    @BeforeEach
    public void setUp() throws Exception {
        final AggregationEventProcessorConfig configMock = mock(AggregationEventProcessorConfig.class);
        when(configMock.validate(any(UserContext.class))).thenReturn(new ValidationResult());
        when(configMock.validate(nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenReturn(new ValidationResult());
        when(configMock.validate(any(UserContext.class), nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenReturn(new ValidationResult());

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
        final ValidationResult validationResult = validate(invalidEventDefinition);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("title");
    }

    @Test
    public void testValidateWithEmptyConfigType() {
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .config(new EventProcessorConfig.FallbackConfig())
            .build();
        final ValidationResult validationResult = validate(invalidEventDefinition);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("config");
    }

    @Test
    public void testValidateWithInvalidConfig() {
        final AggregationEventProcessorConfig configMock = mock(AggregationEventProcessorConfig.class);
        final ValidationResult mockedValidationResult = new ValidationResult();
        mockedValidationResult.addError("foo", "bar");
        when(configMock.validate(any(UserContext.class))).thenReturn(mockedValidationResult);
        when(configMock.validate(nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenReturn(new ValidationResult());
        when(configMock.validate(any(UserContext.class), nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenReturn(new ValidationResult());

        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .config(configMock)
            .build();
        final ValidationResult validationResult = validate(invalidEventDefinition);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("foo");
    }

    @Test
    public void testValidateWithInvalidFieldName() {
        final EventFieldSpec fieldSpecMock = mock(EventFieldSpec.class);
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .fieldSpec(ImmutableMap.of("foo\\bar", fieldSpecMock, "$yo&^a", fieldSpecMock))
            .build();
        final ValidationResult validationResult = validate(invalidEventDefinition);
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
        final ValidationResult validationResult = validate(invalidEventDefinition);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsOnlyKeys("key_spec");
    }

    @Test
    public void testValidEventDefinition() {
        final ValidationResult validationResult = validate(testSubject);
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testTacticsTechniquesCanonicalizedToUpperCaseOnBuild() {
        final EventDefinitionDto dto = testSubject.toBuilder()
                .tacticsTechniques(ImmutableList.of("ta0002", "T1059", "t1059.001"))
                .build();
        assertThat(dto.tacticsTechniques()).containsExactly("TA0002", "T1059", "T1059.001");
    }

    @Test
    public void testValidEventDefinitionWithKeySpecInFieldSpec() {
        final EventFieldSpec fieldSpecMock = mock(EventFieldSpec.class);
        final EventDefinitionDto invalidEventDefinition = testSubject.toBuilder()
            .fieldSpec(ImmutableMap.of("foo", fieldSpecMock, "bar", fieldSpecMock))
            .keySpec(ImmutableList.of("foo", "bar"))
            .build();
        final ValidationResult validationResult = validate(invalidEventDefinition);
        assertThat(validationResult.failed()).isFalse();
        assertThat(validationResult.getErrors().size()).isEqualTo(0);
    }

    @Test
    public void testEventDefinitionHtmlSanitization() throws JsonProcessingException {
        final EventDefinitionDto dto = EventDefinitionDto.builder()
                .title("Test")
                .description("description")
                .priority(1)
                .alert(false)
                .keySpec(ImmutableList.of())
                .config(TestEventProcessorConfig.builder()
                        .message("This is a test event processor")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .notificationSettings(EventNotificationSettings.withGracePeriod(60000))
                .remediationSteps("<form></form> ## Heading")
                .build();
        final String testSubjectJson = new ObjectMapperProvider().get().writeValueAsString(dto);
        // Markdown should be kept.
        assertThat(testSubjectJson).contains("## Heading");
        // HTML should be removed.
        assertThat(testSubjectJson).doesNotContain("form");
    }

    @Test
    public void testValidateCallsAllConfigValidateMethods() {
        final AggregationEventProcessorConfig configMock = mock(AggregationEventProcessorConfig.class);
        when(configMock.validate(any(UserContext.class))).thenReturn(new ValidationResult());
        when(configMock.validate(nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenReturn(new ValidationResult());
        when(configMock.validate(any(UserContext.class), nullable(EventProcessorConfig.class), any(EventDefinitionConfiguration.class)))
                .thenCallRealMethod();

        final EventDefinitionDto dto = testSubject.toBuilder()
                .config(configMock)
                .build();

        final UserContext mockUserContext = mock(UserContext.class);
        final EventDefinitionConfiguration edConfig = new EventDefinitionConfiguration();
        dto.validate(null, edConfig, mockUserContext);

        verify(configMock).validate(mockUserContext);
        verify(configMock).validate(mockUserContext, null, edConfig);
        verify(configMock).validate(null, edConfig);
    }

    private static ValidationResult validate(EventDefinitionDto eventDefinitionDto) {
        return eventDefinitionDto.validate(null, new EventDefinitionConfiguration(), mock(UserContext.class));
    }

    @Test
    public void tagsAreNormalizedOnBuild() {
        final EventDefinitionDto dto = testSubject.toBuilder()
                .tags(ImmutableSet.of("  Phishing  ", "lateral-MOVEMENT", "phishing", "", "   "))
                .build();
        // Trim + lowercase + dedupe + drop blanks
        assertThat(dto.tags()).containsExactlyInAnyOrder("phishing", "lateral-movement");
    }

    @Test
    public void tagsDefaultToEmpty() {
        assertThat(testSubject.tags()).isEmpty();
    }

    @Test
    public void tagsSerializeToJson() throws JsonProcessingException {
        // Build with a real config so Jackson can serialize (testSubject's mock config has
        // Mockito-internal fields that fail serialization).
        final EventDefinitionDto dto = EventDefinitionDto.builder()
                .title("foo")
                .description("bar")
                .priority(1)
                .alert(false)
                .keySpec(ImmutableList.of())
                .config(TestEventProcessorConfig.builder()
                        .message("test")
                        .searchWithinMs(1000)
                        .executeEveryMs(1000)
                        .build())
                .notificationSettings(EventNotificationSettings.withGracePeriod(0))
                .tags(ImmutableSet.of("phishing", "lateral-movement"))
                .build();
        final ObjectMapper mapper = new ObjectMapperProvider().get();
        final JsonNode tree = mapper.readTree(mapper.writeValueAsString(dto));
        assertThat(tree.get("tags")).isNotNull();
        assertThat(tree.get("tags").isArray()).isTrue();
        final List<String> serialized = new ArrayList<>();
        tree.get("tags").forEach(n -> serialized.add(n.asText()));
        assertThat(serialized).containsExactlyInAnyOrder("phishing", "lateral-movement");
    }

    @Test
    public void tagExceedingMaxLengthFailsValidation() {
        final String tooLong = "a".repeat(EventDefinitionDto.MAX_TAG_LENGTH + 1);
        final EventDefinitionDto invalid = testSubject.toBuilder()
                .tags(ImmutableSet.of(tooLong))
                .build();
        final ValidationResult validationResult = validate(invalid);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsKey("tags");
    }

    @Test
    public void tagCountExceedingMaxFailsValidation() {
        final ImmutableSet.Builder<String> tags = ImmutableSet.builder();
        for (int i = 0; i <= EventDefinitionDto.MAX_TAGS; i++) {
            tags.add("tag-" + i);
        }
        final EventDefinitionDto invalid = testSubject.toBuilder()
                .tags(tags.build())
                .build();
        final ValidationResult validationResult = validate(invalid);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsKey("tags");
    }

    @Test
    public void tagWithInvalidCharactersFailsValidation() {
        final EventDefinitionDto invalid = testSubject.toBuilder()
                .tags(ImmutableSet.of("phish:ing"))
                .build();
        final ValidationResult validationResult = validate(invalid);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsKey("tags");
    }

    @Test
    public void invalidTacticsTechniquesAreAllReportedInOneMessage() {
        final EventDefinitionDto invalid = testSubject.toBuilder()
                .tacticsTechniques(ImmutableList.of("TA0002", "bogus", "also-bad", "T1059"))
                .build();
        final ValidationResult validationResult = validate(invalid);
        assertThat(validationResult.failed()).isTrue();
        assertThat(validationResult.getErrors()).containsKey("tactics_techniques");
        final var errors = (java.util.List<String>) validationResult.getErrors().get("tactics_techniques");
        assertThat(errors).hasSize(1);
        // Inputs are uppercased by TacticsTechniquesNormalizer before validation, so the
        // error message reflects the canonical (upper-cased) form.
        assertThat(errors.get(0))
                .contains("\"BOGUS\"")
                .contains("\"ALSO-BAD\"")
                .doesNotContain("\"TA0002\"")
                .doesNotContain("\"T1059\"");
    }

    @Test
    public void tagWithDotIsValid() {
        // The test fixture may have unrelated validation errors, so we only assert that
        // no tags-specific error is reported.
        final EventDefinitionDto valid = testSubject.toBuilder()
                .tags(ImmutableSet.of("attack.t1110", "auth.failed"))
                .build();
        final ValidationResult validationResult = validate(valid);
        assertThat(validationResult.getErrors()).doesNotContainKey("tags");
    }
}
