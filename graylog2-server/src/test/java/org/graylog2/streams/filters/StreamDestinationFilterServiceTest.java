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
package org.graylog2.streams.filters;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.client.model.Sorts;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.streams.events.StreamDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class StreamDestinationFilterServiceTest {

    @Mock
    private DestinationFilterCreationValidator mockedFilterLicenseCheck;
    private StreamDestinationFilterService service;
    private EventBus eventBus;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.eventBus = new EventBus("stream-destination-filter-service");
        this.service = new StreamDestinationFilterService(mongoCollections, new ClusterEventBus(MoreExecutors.directExecutor()), eventBus, Optional.of(mockedFilterLicenseCheck));
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void findPaginatedForStream() {
        final var result = service.findPaginatedForStream("54e3deadbeefdeadbeef1000", "", Sorts.ascending("title"), 10, 1, id -> true);

        assertThat(result.delegate()).hasSize(3);
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void findPaginatedForStreamWithQuery() {
        final var result = service.findPaginatedForStream("54e3deadbeefdeadbeef1000", "title:\"Test Filter 2\"", Sorts.ascending("title"), 10, 1, id -> true);

        assertThat(result.delegate()).hasSize(1);
        assertThat(result.delegate().get(0).title()).isEqualTo("Test Filter 2");
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void findPaginatedForStreamAndTarget() {
        final var result = service.findPaginatedForStreamAndTarget("54e3deadbeefdeadbeef1000", "indexer", "", Sorts.ascending("title"), 10, 1, id -> true);

        assertThat(result.delegate()).hasSize(2);
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void findPaginatedForStreamAndTargetWithQuery() {
        final var result = service.findPaginatedForStreamAndTarget("54e3deadbeefdeadbeef1000", "indexer", "status:disabled", Sorts.ascending("title"), 10, 1, id -> true);

        assertThat(result.delegate()).hasSize(1);
        assertThat(result.delegate().get(0).status()).isEqualTo(StreamDestinationFilterRuleDTO.Status.DISABLED);
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void findByIdForStream() {
        assertThat(service.findByIdForStream("foo", "54e3deadbeefdeadbeef0000")).get().satisfies(dto -> {
            assertThat(dto.title()).isEqualTo("Test Filter 1");
            assertThat(dto.streamId()).isEqualTo("54e3deadbeefdeadbeef1000");
            assertThat(dto.destinationType()).isEqualTo("indexer");
            assertThat(dto.status()).isEqualTo(StreamDestinationFilterRuleDTO.Status.ENABLED);
            assertThat(dto.rule()).satisfies(rule -> {
                assertThat(rule.operator()).isEqualTo(RuleBuilderStep.Operator.OR);
                assertThat(rule.conditions()).hasSize(2);
                assertThat(rule.actions()).isNull();
            });
        });
    }

    @Test
    void createForStream() {
        final var result = service.createForStream("stream-1", StreamDestinationFilterRuleDTO.builder()
                .title("Test")
                .description("A Test")
                .streamId("stream-1")
                .destinationType("indexer")
                .status(StreamDestinationFilterRuleDTO.Status.DISABLED)
                .rule(RuleBuilder.builder()
                        .operator(RuleBuilderStep.Operator.AND)
                        .conditions(List.of(
                                RuleBuilderStep.builder()
                                        .function("has_field")
                                        .parameters(Map.of("field", "is_debug"))
                                        .build()
                        ))
                        .build())
                .build());

        assertThat(result.id()).isNotBlank();
        assertThat(result.title()).isEqualTo("Test");
        assertThat(result.description()).get().isEqualTo("A Test");
        assertThat(result.streamId()).isEqualTo("stream-1");
        assertThat(result.destinationType()).isEqualTo("indexer");
        assertThat(result.status()).isEqualTo(StreamDestinationFilterRuleDTO.Status.DISABLED);
        assertThat(result.rule()).satisfies(rule -> {
            assertThat(rule.operator()).isEqualTo(RuleBuilderStep.Operator.AND);
            assertThat(rule.conditions()).hasSize(1);
        });
    }

    @Test
    void createForStreamThrowsExceptionWhenLicenseCheckFails() throws IllegalStateException {
        StreamDestinationFilterRuleDTO filterDto = StreamDestinationFilterRuleDTO.builder()
                .title("Test")
                .description("A Test")
                .streamId("stream-1")
                .destinationType("checked-destination")
                .status(StreamDestinationFilterRuleDTO.Status.DISABLED)
                .rule(RuleBuilder.builder()
                        .operator(RuleBuilderStep.Operator.AND)
                        .conditions(List.of(
                                RuleBuilderStep.builder()
                                        .function("has_field")
                                        .parameters(Map.of("field", "is_debug"))
                                        .build()
                        ))
                        .build())
                .build();
        doThrow(new IllegalStateException("Invalid action!")).when(mockedFilterLicenseCheck).validate(filterDto);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> service.createForStream("stream-1", filterDto));
        assertThat(exception.getMessage()).contains("Invalid action!");
    }

    @Test
    void createForStreamWithExistingID() {
        final StreamDestinationFilterRuleDTO dto = StreamDestinationFilterRuleDTO.builder()
                .id("54e3deadbeefdeadbeef0000")
                .title("Test")
                .description("A Test")
                .streamId("stream-1")
                .destinationType("indexer")
                .status(StreamDestinationFilterRuleDTO.Status.DISABLED)
                .rule(RuleBuilder.builder()
                        .operator(RuleBuilderStep.Operator.AND)
                        .conditions(List.of(
                                RuleBuilderStep.builder()
                                        .function("has_field")
                                        .parameters(Map.of("field", "is_debug"))
                                        .build()
                        ))
                        .build())
                .build();

        assertThatThrownBy(() -> service.createForStream("stream-1", dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createForStreamEnforcesGivenStreamID() {
        final var result = service.createForStream("stream-1", StreamDestinationFilterRuleDTO.builder()
                .title("Test")
                .description("A Test")
                .streamId("stream-no-no")
                .destinationType("indexer")
                .status(StreamDestinationFilterRuleDTO.Status.DISABLED)
                .rule(RuleBuilder.builder()
                        .operator(RuleBuilderStep.Operator.AND)
                        .conditions(List.of(
                                RuleBuilderStep.builder()
                                        .function("has_field")
                                        .parameters(Map.of("field", "is_debug"))
                                        .build()
                        ))
                        .build())
                .build());

        assertThat(result.streamId()).isEqualTo("stream-1");
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void updateForStream() {
        final var optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(optionalDto).isPresent().get().satisfies(d -> {
            assertThat(d.title()).isEqualTo("Test Filter 1");
        });

        final var dto = optionalDto.get();

        final var updatedDto = service.updateForStream(dto.streamId(), dto.toBuilder().title("Changed title").build());
        final var reloadedUpdatedDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(updatedDto).satisfies(d -> {
            assertThat(d.title()).isEqualTo("Changed title");
        });
        assertThat(reloadedUpdatedDto).get().satisfies(d -> {
            assertThat(d.title()).isEqualTo("Changed title");
        });
    }

    @Test

    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void updateForStreamEnforcesGivenStreamID() {
        final var optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(optionalDto).isPresent().get().satisfies(d -> {
            assertThat(d.title()).isEqualTo("Test Filter 1");
        });

        final var dto = optionalDto.get();

        final var updatedDto = service.updateForStream("54e3deadbeefdeadbeef1000", dto.toBuilder()
                .title("Changed title")
                .streamId("custom-stream")
                .build());
        final var reloadedUpdatedDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(updatedDto).satisfies(d -> {
            assertThat(d.streamId()).isEqualTo("54e3deadbeefdeadbeef1000");
            assertThat(d.title()).isEqualTo("Changed title");
        });
        assertThat(reloadedUpdatedDto).get().satisfies(d -> {
            assertThat(d.streamId()).isEqualTo("54e3deadbeefdeadbeef1000");
            assertThat(d.title()).isEqualTo("Changed title");
        });
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void deleteFromStream() {
        final var optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(optionalDto).isPresent();

        final var deletedDto = service.deleteFromStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");

        assertThat(deletedDto.id()).isEqualTo("54e3deadbeefdeadbeef0000");

        assertThat(service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000")).isNotPresent();
    }

    @Test
    void deleteFromStreamWithInvalidID() {
        assertThatThrownBy(() -> service.deleteFromStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef9999"))
                .hasMessageContaining("54e3deadbeefdeadbeef9999")
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void forEachEnabled() {
        final var resultBuilder = ImmutableMultimap.<String, String>builder();

        service.forEachEnabledFilterGroupedByStream(filter -> filter.filters().forEach(f -> resultBuilder.put(filter.streamId(), f.title())));

        final var result = resultBuilder.build();

        assertThat(result.keySet()).containsExactlyInAnyOrder("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef2000");
        assertThat(result.get("54e3deadbeefdeadbeef1000")).containsExactlyInAnyOrder("Test Filter 1", "Test Filter 3");
        assertThat(result.get("54e3deadbeefdeadbeef2000")).containsExactlyInAnyOrder("Test Filter 4");
    }

    @Test
    @MongoDBFixtures("StreamDestinationFilterServiceTest-2024-07-01-1.json")
    void streamDeletionEvent() {
        var optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");
        assertThat(optionalDto).isPresent();

        optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0001");
        assertThat(optionalDto).isPresent();

        optionalDto = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0002");
        assertThat(optionalDto).isPresent();

        eventBus.post(StreamDeletedEvent.create("54e3deadbeefdeadbeef1000"));

        var afterDeletionEvent = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0000");
        assertThat(afterDeletionEvent).isNotPresent();

        afterDeletionEvent = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0001");
        assertThat(afterDeletionEvent).isNotPresent();

        afterDeletionEvent = service.findByIdForStream("54e3deadbeefdeadbeef1000", "54e3deadbeefdeadbeef0002");
        assertThat(afterDeletionEvent).isNotPresent();
    }
}
