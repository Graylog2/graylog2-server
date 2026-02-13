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
package org.graylog2.database;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.plugin.system.SimpleNodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
class MongoSequenceServiceTest {

    private static final String TOPIC_A = "topic_a";
    private static final String TOPIC_B = "topic_b";
    private static final NodeId NODE_ID = new SimpleNodeId("test-node-1");

    private MongoSequenceService service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new MongoSequenceService(
                mongoCollections,
                NODE_ID,
                Set.of(TOPIC_A, TOPIC_B)
        );
    }

    @Test
    void incrementAndGetReturnsOneOnFirstCall() {
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(1L);
    }

    @Test
    void incrementAndGetReturnsConsecutiveValues() {
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(1L);
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(2L);
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(3L);
    }

    @Test
    void incrementAndGetProducesIndependentSequencesPerTopic() {
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(1L);
        assertThat(service.incrementAndGet(TOPIC_B)).isEqualTo(1L);
        assertThat(service.incrementAndGet(TOPIC_A)).isEqualTo(2L);
        assertThat(service.incrementAndGet(TOPIC_B)).isEqualTo(2L);
    }

    @Test
    void getCurrentValueReturnsZeroForNeverIncrementedTopic() {
        assertThat(service.getCurrentValue(TOPIC_A)).isEqualTo(0L);
    }

    @Test
    void getCurrentValueReturnsCorrectValueAfterIncrements() {
        service.incrementAndGet(TOPIC_A);
        service.incrementAndGet(TOPIC_A);
        service.incrementAndGet(TOPIC_A);

        assertThat(service.getCurrentValue(TOPIC_A)).isEqualTo(3L);
    }

    @Test
    void getCurrentValueDoesNotIncrementTheCounter() {
        service.incrementAndGet(TOPIC_A);
        service.getCurrentValue(TOPIC_A);
        service.getCurrentValue(TOPIC_A);

        assertThat(service.getCurrentValue(TOPIC_A)).isEqualTo(1L);
    }

    @Test
    void incrementAndGetThrowsForUnregisteredTopic() {
        assertThatThrownBy(() -> service.incrementAndGet("unknown_topic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown_topic");
    }

    @Test
    void getCurrentValueThrowsForUnregisteredTopic() {
        assertThatThrownBy(() -> service.getCurrentValue("unknown_topic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown_topic");
    }

    @Test
    void incrementAndGetSetsMetadataFields() {
        service.incrementAndGet(TOPIC_A);

        var rawCollection = service.getCollection();
        var doc = rawCollection.find(com.mongodb.client.model.Filters.eq("_id", TOPIC_A)).first();

        assertThat(doc).isNotNull();
        assertThat(doc.get(MongoSequenceService.FIELD_LAST_UPDATED_AT)).isNotNull();
        assertThat(doc.getString(MongoSequenceService.FIELD_LAST_UPDATED_BY)).isEqualTo("test-node-1");
    }
}
