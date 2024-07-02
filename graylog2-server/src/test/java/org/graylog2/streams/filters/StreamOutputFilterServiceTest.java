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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
class StreamOutputFilterServiceTest {

    private StreamOutputFilterService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        this.service = new StreamOutputFilterService(mongoCollections);
    }

    @Test
    @MongoDBFixtures("StreamOutputFilterServiceTest-2024-07-01-1.json")
    void findPaginated() {
        final var result = service.findPaginated(Filters.empty(), Sorts.ascending("title"), 10, 1, id -> true);

        assertThat(result.delegate()).hasSize(2);
    }

    @Test
    @MongoDBFixtures("StreamOutputFilterServiceTest-2024-07-01-1.json")
    void findById() {
        assertThat(service.findById("54e3deadbeefdeadbeef0000")).get().satisfies(dto -> {
            assertThat(dto.title()).isEqualTo("Test Filter 1");
            assertThat(dto.streamId()).isEqualTo("54e3deadbeefdeadbeef1000");
            assertThat(dto.outputTarget()).isEqualTo("indexer");
            assertThat(dto.status()).isEqualTo(StreamOutputFilterRuleDTO.Status.ENABLED);
            assertThat(dto.rule()).satisfies(rule -> {
                assertThat(rule.operator()).isEqualTo(RuleBuilderStep.Operator.OR);
                assertThat(rule.conditions()).hasSize(2);
                assertThat(rule.actions()).isNull();
            });
        });
    }

    @Test
    void create() {
        final var result = service.create(StreamOutputFilterRuleDTO.builder()
                .title("Test")
                .description("A Test")
                .streamId("stream-1")
                .outputTarget("indexer")
                .status(StreamOutputFilterRuleDTO.Status.DISABLED)
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
        assertThat(result.outputTarget()).isEqualTo("indexer");
        assertThat(result.status()).isEqualTo(StreamOutputFilterRuleDTO.Status.DISABLED);
        assertThat(result.rule()).satisfies(rule -> {
            assertThat(rule.operator()).isEqualTo(RuleBuilderStep.Operator.AND);
            assertThat(rule.conditions()).hasSize(1);
        });
    }

    @Test
    void createWithExistingID() {
        final StreamOutputFilterRuleDTO dto = StreamOutputFilterRuleDTO.builder()
                .id("54e3deadbeefdeadbeef0000")
                .title("Test")
                .description("A Test")
                .streamId("stream-1")
                .outputTarget("indexer")
                .status(StreamOutputFilterRuleDTO.Status.DISABLED)
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

        assertThatThrownBy(() -> service.create(dto)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update() {
    }
}
