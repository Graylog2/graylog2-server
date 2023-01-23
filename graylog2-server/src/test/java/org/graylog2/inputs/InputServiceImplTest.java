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
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_CONFIGURATION;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_CREATED_AT;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_CREATOR_USER_ID;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_TITLE;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_TYPE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    private EncryptedValueService encryptedValueService;

    @Before
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    public void setUp() throws Exception {
        clusterEventBus = new ClusterEventBus("inputs-test", Executors.newSingleThreadExecutor());
        encryptedValueService = new EncryptedValueService(UUID.randomUUID().toString());
        inputService = new InputServiceImpl(
                mongodb.mongoConnection(),
                extractorFactory,
                converterFactory,
                messageInputFactory,
                clusterEventBus,
                new ObjectMapperProvider().get());
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

    @Test
    public void handlesEncryptedValue() throws ValidationException, NotFoundException {

        // Setup required to detect fields that need conversion from Map to EncryptedValue when reading
        final MessageInput.Config inputConfig = mock(MessageInput.Config.class);
        when(inputConfig.combinedRequestedConfiguration()).thenReturn(ConfigurationRequest.createWithFields(
                new TextField("encrypted", "", "", "",
                        ConfigurationField.Optional.OPTIONAL, true)
        ));
        when(messageInputFactory.getConfig("test type")).thenReturn(Optional.of(
                inputConfig
        ));

        final EncryptedValue secret = encryptedValueService.encrypt("secret");
        final String id = inputService.save(new InputImpl(Map.of(
                FIELD_TYPE, "test type",
                FIELD_TITLE, "test title",
                FIELD_CREATED_AT, new Date(),
                FIELD_CREATOR_USER_ID, "test creator",
                FIELD_CONFIGURATION, Map.of(
                        "encrypted", secret
                )
        )));

        assertThat(id).isNotBlank();

        final Input input = inputService.find(id);
        assertThat(input.getConfiguration()).hasEntrySatisfying("encrypted", value -> {
            assertThat(value).isInstanceOf(EncryptedValue.class);
            assertThat(value).isEqualTo(secret);
        });
    }

}
