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
package org.graylog2.inputs;

import com.google.common.collect.ImmutableSet;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InputServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();


    @Mock
    private ExtractorFactory extractorFactory;

    @Mock
    private ConverterFactory converterFactory;

    @Mock
    private MessageInputFactory messageInputFactory;

    private ClusterEventBus clusterEventBus;
    private InputServiceImpl inputService;

    @Before
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    public void setUp() throws Exception {
        clusterEventBus = new ClusterEventBus("inputs-test", Executors.newSingleThreadExecutor());
        inputService = new InputServiceImpl(
                mongodb.mongoConnection(),
                extractorFactory,
                converterFactory,
                messageInputFactory,
                clusterEventBus
        );
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void allReturnsAllInputs() {
        final List<Input> inputs = inputService.all();
        assertThat(inputs).hasSize(3);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void allOfThisNodeReturnsAllLocalAndGlobalInputs() {
        final List<Input> inputs = inputService.allOfThisNode("cd03ee44-b2a7-cafe-babe-0000deadbeef");
        assertThat(inputs).hasSize(3);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void allOfThisNodeReturnsGlobalInputsIfNodeIDDoesNotExist() {
        final List<Input> inputs = inputService.allOfThisNode("cd03ee44-b2a7-0000-0000-000000000000");
        assertThat(inputs).hasSize(1);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void findByIdsReturnsRequestedInputs() {
        assertThat(inputService.findByIds(ImmutableSet.of())).isEmpty();
        assertThat(inputService.findByIds(ImmutableSet.of("54e300000000000000000000"))).isEmpty();
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001"))).hasSize(1);
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0003"))).hasSize(2);
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0003", "54e300000000000000000000"))).hasSize(2);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void findReturnsExistingInput() throws NotFoundException {
        final Input input = inputService.find("54e3deadbeefdeadbeef0002");
        assertThat(input.getId()).isEqualTo("54e3deadbeefdeadbeef0002");
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void findThrowsNotFoundExceptionIfInputDoesNotExist() {
        assertThatThrownBy(() -> inputService.find("54e300000000000000000000"))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void globalCountReturnsNumberOfGlobalInputs() {
        assertThat(inputService.globalCount()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void localCountReturnsNumberOfLocalInputs() {
        assertThat(inputService.localCount()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    public void localCountForNodeReturnsNumberOfLocalInputs() {
        assertThat(inputService.localCountForNode("cd03ee44-b2a7-cafe-babe-0000deadbeef")).isEqualTo(2);
        assertThat(inputService.localCountForNode("cd03ee44-b2a7-0000-0000-000000000000")).isEqualTo(0);
    }
}
