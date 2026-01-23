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
import com.mongodb.client.model.Filters;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class InputServiceImplTest {

    @Mock
    private ExtractorFactory extractorFactory;

    @Mock
    private ConverterFactory converterFactory;

    @Mock
    private MessageInputFactory messageInputFactory;


    private ClusterEventBus clusterEventBus;
    private InputServiceImpl inputService;
    private EncryptedValueService encryptedValueService;

    @BeforeEach
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    void setUp(MongoCollections mongoCollections) {
        clusterEventBus = new ClusterEventBus("inputs-test", Executors.newSingleThreadExecutor());
        encryptedValueService = new EncryptedValueService(UUID.randomUUID().toString());
        inputService = new InputServiceImpl(
                mongoCollections,
                extractorFactory,
                converterFactory,
                messageInputFactory,
                clusterEventBus,
                new ObjectMapperProvider().get());
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void allReturnsAllInputs() {
        final List<Input> inputs = inputService.all();
        assertThat(inputs).hasSize(3);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void allOfThisNodeReturnsAllLocalAndGlobalInputs() {
        final List<Input> inputs = inputService.allOfThisNode("cd03ee44-b2a7-cafe-babe-0000deadbeef");
        assertThat(inputs).hasSize(3);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void allOfThisNodeReturnsGlobalInputsIfNodeIDDoesNotExist() {
        final List<Input> inputs = inputService.allOfThisNode("cd03ee44-b2a7-0000-0000-000000000000");
        assertThat(inputs).hasSize(1);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void findByIdsReturnsRequestedInputs() {
        assertThat(inputService.findByIds(ImmutableSet.of())).isEmpty();
        assertThat(inputService.findByIds(ImmutableSet.of("54e300000000000000000000"))).isEmpty();
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001"))).hasSize(1);
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0003"))).hasSize(2);
        assertThat(inputService.findByIds(ImmutableSet.of("54e3deadbeefdeadbeef0001", "54e3deadbeefdeadbeef0003", "54e300000000000000000000"))).hasSize(2);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void findReturnsExistingInput() throws NotFoundException {
        final Input input = inputService.find("54e3deadbeefdeadbeef0002");
        assertThat(input.getId()).isEqualTo("54e3deadbeefdeadbeef0002");
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void findThrowsNotFoundExceptionIfInputDoesNotExist() {
        assertThatThrownBy(() -> inputService.find("54e300000000000000000000"))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void globalCountReturnsNumberOfGlobalInputs() {
        assertThat(inputService.globalCount()).isEqualTo(1);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void localCountReturnsNumberOfLocalInputs() {
        assertThat(inputService.localCount()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void localCountForNodeReturnsNumberOfLocalInputs() {
        assertThat(inputService.localCountForNode("cd03ee44-b2a7-cafe-babe-0000deadbeef")).isEqualTo(2);
        assertThat(inputService.localCountForNode("cd03ee44-b2a7-0000-0000-000000000000")).isZero();
    }

    @Test
    void handlesEncryptedValue() throws ValidationException, NotFoundException {

        // Setup required to detect fields that need conversion from Map to EncryptedValue when reading
        final MessageInput.Config inputConfig = mock(MessageInput.Config.class);
        when(inputConfig.combinedRequestedConfiguration()).thenReturn(ConfigurationRequest.createWithFields(
                new TextField("encrypted", "", "", "",
                        ConfigurationField.Optional.OPTIONAL, true),
                new TextField("encrypted2", "", "", "",
                        ConfigurationField.Optional.OPTIONAL, true)
        ));
        when(messageInputFactory.getConfig("test type")).thenReturn(Optional.of(
                inputConfig
        ));

        final EncryptedValue secret = encryptedValueService.encrypt("secret");
        final EncryptedValue secret2 = encryptedValueService.encrypt("secret2");
        final InputImpl newInput = InputImpl.builder()
                .setTitle("test title")
                .setType("test type")
                .setCreatorUserId("test creator")
                .setCreatedAt(new DateTime(DateTimeZone.UTC))
                .setConfiguration(Map.of(
                        "encrypted", secret,
                        "encrypted2", secret2
                ))
                .build();
        final String id = inputService.save(newInput);

        assertThat(id).isNotBlank();

        assertThat(inputService.find(id)).satisfies(input -> {
            Map<String, Object> configuration = input.getConfiguration();
            assertThat(configuration).hasEntrySatisfying("encrypted", value -> {
                assertThat(value).isInstanceOf(EncryptedValue.class);
                assertThat(value).isEqualTo(secret);
            });
            assertThat(configuration).hasEntrySatisfying("encrypted2", value -> {
                assertThat(value).isInstanceOf(EncryptedValue.class);
                assertThat(value).isEqualTo(secret2);
            });
        });

        assertThat(inputService.allByType("test type")).hasSize(1).first().satisfies(input ->
                assertThat(input.getConfiguration()).hasEntrySatisfying("encrypted", value -> {
                    assertThat(value).isInstanceOf(EncryptedValue.class);
                    assertThat(value).isEqualTo(secret);
                }));
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void findByTitle() {
        String rawTcp = "Raw TCP";
        List<String> idsByTitle = inputService.findIdsByTitle(rawTcp);
        assertThat(idsByTitle).hasSize(1);
    }

    @Test
    void createInput() {
        Map<String, Object> localFields = Map.of(
                MessageInput.FIELD_TYPE, "test type",
                MessageInput.FIELD_TITLE, "test title",
                MessageInput.FIELD_CREATOR_USER_ID, "creator-1",
                MessageInput.FIELD_DESIRED_STATE, IOState.Type.RUNNING.name(),
                MessageInput.FIELD_CONTENT_PACK, "content-pack-1",
                MessageInput.FIELD_CONFIGURATION, Map.of("foo", "bar"),
                MessageInput.FIELD_GLOBAL, false,
                MessageInput.FIELD_NODE_ID, "node-123",
                MessageInput.FIELD_STATIC_FIELDS, List.of(
                        Map.of(InputImpl.FIELD_STATIC_FIELD_KEY, "static_key",
                                InputImpl.FIELD_STATIC_FIELD_VALUE, "static_value")
                )
        );

        Input result = inputService.create(localFields);
        assertThat(result.getId()).isNull();
        assertThat(result.getType()).isEqualTo("test type");
        assertThat(result.getTitle()).isEqualTo("test title");
        assertThat(result.getCreatorUserId()).isEqualTo("creator-1");
        assertThat(result.getDesiredState()).isEqualTo(IOState.Type.RUNNING);
        assertThat(result.getContentPack()).isEqualTo("content-pack-1");
        assertThat(result.getConfiguration()).containsEntry("foo", "bar");
        assertThat(result.isGlobal()).isFalse();
        assertThat(result.getNodeId()).isEqualTo("node-123");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getStaticFields()).containsEntry("static_key", "static_value");

    }

    @Test
    void inputWithOutDesiredStateDefaultsToRunning() {
        Map<String, Object> localFields = Map.of(
                MessageInput.FIELD_TYPE, "test type",
                MessageInput.FIELD_TITLE, "test title",
                MessageInput.FIELD_CREATOR_USER_ID, "creator-1",
                MessageInput.FIELD_CONFIGURATION, Map.of("foo", "bar")
        );

        Input result = inputService.create(localFields);
        assertThat(result.getDesiredState()).isEqualTo(IOState.Type.RUNNING);
    }

    @Test
    void saveInput() throws Exception {
        InputImpl newInput = createTestInput();

        String id = inputService.save(newInput);
        Input savedInput = inputService.find(id);

        assertEquals(newInput.getTitle(), savedInput.getTitle());
        assertEquals(newInput.getType(), savedInput.getType());
        assertEquals(newInput.getCreatorUserId(), savedInput.getCreatorUserId());
        assertEquals(newInput.getDesiredState(), savedInput.getDesiredState());
        assertEquals(newInput.isGlobal(), savedInput.isGlobal());
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void testExtractor() throws Exception {
        Input input = inputService.find("54e3deadbeefdeadbeef0001");

        String extractorId = new ObjectId().toHexString();
        Extractor extractor = Mockito.mock(Extractor.class);
        Map<String, Object> persistedFields = new HashMap<>();
        persistedFields.put(Extractor.FIELD_ID, extractorId);
        String extractorTitle = "extractor title";
        persistedFields.put(Extractor.FIELD_TITLE, extractorTitle);
        persistedFields.put(Extractor.FIELD_ORDER, 0);
        persistedFields.put(Extractor.FIELD_CURSOR_STRATEGY, Extractor.CursorStrategy.COPY.name());
        persistedFields.put(Extractor.FIELD_TYPE, Extractor.Type.GROK.name());
        persistedFields.put(Extractor.FIELD_SOURCE_FIELD, "message");
        persistedFields.put(Extractor.FIELD_TARGET_FIELD, "message");
        persistedFields.put(Extractor.FIELD_EXTRACTOR_CONFIG, Map.of());
        persistedFields.put(Extractor.FIELD_CREATOR_USER_ID, "user-x");
        persistedFields.put(Extractor.FIELD_CONVERTERS, List.of());
        persistedFields.put(Extractor.FIELD_CONDITION_TYPE, Extractor.ConditionType.STRING.name());


        when(extractor.getId()).thenReturn(extractorId);
        when(extractor.getTitle()).thenReturn(extractorTitle);
        when(extractor.getType()).thenReturn(Extractor.Type.GROK);
        when(extractor.getCursorStrategy()).thenReturn(Extractor.CursorStrategy.COPY);
        when(extractor.getSourceField()).thenReturn("message");
        when(extractor.getTargetField()).thenReturn("message");
        when(extractor.getCreatorUserId()).thenReturn("user-x");
        when(extractor.getExtractorConfig()).thenReturn(Map.of());
        when(extractor.getConditionType()).thenReturn(Extractor.ConditionType.STRING);
        when(extractor.getConditionValue()).thenReturn("");
        when(extractor.getPersistedFields()).thenReturn(persistedFields);
        when(extractorFactory.factory(any(), any(), anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(extractor);

        inputService.addExtractor(input, extractor);

        assertThat(inputService.getExtractors(input.getId())).hasSize(1);
        Extractor extractorResult = inputService.getExtractor(input, extractorId);
        assertThat(extractorResult.getTitle()).isEqualTo(extractorTitle);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void testDeleteExtractor() throws Exception {
        Input input = inputService.find("54e3deadbeefdeadbeef0002");
        Extractor extractor = Mockito.mock(Extractor.class);
        when(extractorFactory.factory(any(), any(), anyLong(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(extractor);
        assertThat(inputService.getExtractors(input.getId())).hasSize(1);
        inputService.removeExtractor(input, "4ec88750-c522-11f0-bdff-9eee7e74cea5");
        assertThat(inputService.getExtractors(input.getId())).isEmpty();
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void testPaginated() {
        PaginatedList<Input> paginated = inputService.paginated(Filters.empty(), input -> true, SortOrder.ASCENDING, InputImpl.FIELD_TITLE, 1, 2);

        assertThat(paginated).isNotNull();
        assertThat(paginated.pagination().total()).isEqualTo(3);
        assertThat((long) paginated.size()).isEqualTo(2);
    }

    @Test
    @MongoDBFixtures("InputServiceImplTest.json")
    void testStaticFields() {
        assertThat(inputService.getStaticFields("54e3deadbeefdeadbeef0002")).isEmpty();
        List<Map.Entry<String, String>> staticFields = inputService.getStaticFields("54e3deadbeefdeadbeef0003");
        assertThat(staticFields).isNotNull().hasSize(1).isEqualTo(List.of(Map.entry("static_field", "foo")));
    }

    private InputImpl createTestInput() {
        return InputImpl.builder()
                .setTitle("input title")
                .setType("prototype")
                .setCreatorUserId("admin")
                .setCreatedAt(Tools.nowUTC())
                .setConfiguration(Map.of("k", "v"))
                .setPersistedDesiredState(IOState.Type.RUNNING)
                .setGlobal(true)
                .build();
    }
}
