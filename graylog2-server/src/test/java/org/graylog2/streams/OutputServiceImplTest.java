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
package org.graylog2.streams;

import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.outputs.events.OutputChangedEvent;
import org.graylog2.plugin.streams.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class OutputServiceImplTest {

    @Mock
    private StreamService streamService;
    @Mock
    private ClusterEventBus clusterEventBus;

    private OutputServiceImpl outputService;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) throws Exception {
        outputService = new OutputServiceImpl(
                mongoCollections,
                streamService,
                clusterEventBus);
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void loadAllReturnsAllOutputs() {
        final Set<Output> outputs = outputService.loadAll();
        assertThat(outputs).hasSize(2);
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void loadByIdsReturnsRequestedOutputs() {
        assertThat(outputService.loadByIds(ImmutableSet.of())).isEmpty();
        assertThat(outputService.loadByIds(ImmutableSet.of("54e300000000000000000000"))).isEmpty();
        assertThat(outputService.loadByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e300000000000000000000"))).hasSize(1);
        assertThat(outputService.loadByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0002", "54e300000000000000000000"))).hasSize(2);
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void loadReturnsExistingOutput() throws NotFoundException {
        final Output output = outputService.load("54e3deadbeefdeadbeef0001");
        assertThat(output.getId()).isEqualTo("54e3deadbeefdeadbeef0001");
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void loadThrowsNotFoundExceptionForNonExistingOutput() {
        assertThatThrownBy(() -> outputService.load("54e300000000000000000000"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void countReturnsNumberOfOutputs() {
        assertThat(outputService.count()).isEqualTo(2L);
    }

    @Test
    @MongoDBFixtures("OutputServiceImplTest.json")
    public void countByTypeReturnsNumberOfOutputsByType() {
        assertThat(outputService.countByType())
                .hasSize(2)
                .containsEntry("org.graylog2.outputs.LoggingOutput", 1L)
                .containsEntry("org.graylog2.outputs.GelfOutput", 1L);
    }

    @Test
    @MongoDBFixtures("single-output.json")
    public void updatingOutputIsPersistent() throws Exception {
        final String outputId = "5b927d32a7c8644ed44576ed";
        final Output newOutput = outputService.update(outputId, Collections.singletonMap("title", "Some other Title"));

        assertThat(newOutput.getTitle()).isEqualTo("Some other Title");

        final Output retrievedOutput = outputService.load(outputId);

        assertThat(retrievedOutput.getTitle()).isEqualTo("Some other Title");
    }

    @Test
    @MongoDBFixtures("single-output.json")
    public void updatingEmitsEvent() throws Exception {
        final String outputId = "5b927d32a7c8644ed44576ed";
        outputService.update(outputId, Collections.singletonMap("title", "Some other Title"));

        verify(clusterEventBus).post(OutputChangedEvent.create(outputId));
    }
}
