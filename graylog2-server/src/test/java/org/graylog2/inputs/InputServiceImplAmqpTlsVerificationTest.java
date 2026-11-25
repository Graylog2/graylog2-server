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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.inputs.raw.amqp.RawAMQPInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.transports.AmqpTransport;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.inputs.transports.AmqpTransport.CK_TLS_VERIFICATION_ENABLED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Inputs created before the {@link AmqpTransport#CK_TLS_VERIFICATION_ENABLED} setting existed must keep working
 * without TLS certificate verification, while new inputs verify certificates.
 */
@ExtendWith(MongoDBExtension.class)
class InputServiceImplAmqpTlsVerificationTest {
    private static final String AMQP_TYPE = RawAMQPInput.class.getCanonicalName();
    private static final String OTHER_TYPE = RawTCPInput.class.getCanonicalName();

    private static final String LEGACY_ID = "6a1e00000000000000000001";
    private static final String NULL_ID = "6a1e00000000000000000002";
    private static final String ENABLED_ID = "6a1e00000000000000000003";
    private static final String OTHER_ID = "6a1e00000000000000000004";

    private InputServiceImpl inputService;
    private MongoCollection<Document> rawCollection;

    @BeforeEach
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    void setUp(MongoCollections mongoCollections) {
        final MessageInputFactory messageInputFactory = mock(MessageInputFactory.class);
        when(messageInputFactory.getConfig(AMQP_TYPE)).thenReturn(Optional.of(amqpInputConfig()));
        when(messageInputFactory.getConfig(OTHER_TYPE)).thenReturn(Optional.of(otherInputConfig()));

        inputService = new InputServiceImpl(
                mongoCollections,
                mock(ExtractorFactory.class),
                mock(ConverterFactory.class),
                messageInputFactory,
                new ClusterEventBus("inputs-test", Executors.newSingleThreadExecutor()),
                new ObjectMapperProvider().get());
        rawCollection = mongoCollections.nonEntityCollection(InputServiceImpl.COLLECTION_NAME, Document.class);
    }

    @Test
    @MongoDBFixtures("amqp-tls-verification-inputs.json")
    void findReturnsDisabledVerificationForInputsWithoutSetting() throws Exception {
        assertThat(inputService.find(LEGACY_ID).getConfiguration()).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
        assertThat(inputService.find(NULL_ID).getConfiguration()).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
        assertThat(inputService.find(ENABLED_ID).getConfiguration()).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
    }

    @Test
    @MongoDBFixtures("amqp-tls-verification-inputs.json")
    void allReadMethodsReturnDisabledVerificationForInputsWithoutSetting() {
        assertExpectedVerificationSettings(inputService.all());
        assertExpectedVerificationSettings(inputService.allByType(AMQP_TYPE));
        assertExpectedVerificationSettings(inputService.allOfThisNode("some-node"));
        assertExpectedVerificationSettings(inputService.findByIds(Set.of(LEGACY_ID, NULL_ID, ENABLED_ID, OTHER_ID)));
        assertExpectedVerificationSettings(inputService.paginated(Filters.empty(), input -> true, SortOrder.ASCENDING, InputImpl.FIELD_TITLE, 1, 10));
    }

    @Test
    @MongoDBFixtures("amqp-tls-verification-inputs.json")
    void readsDoNotModifyPersistedInputs() throws Exception {
        inputService.find(LEGACY_ID);
        inputService.all();
        inputService.allByType(AMQP_TYPE);

        assertThat(persistedConfiguration(LEGACY_ID)).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);
        assertThat(persistedConfiguration(NULL_ID)).containsEntry(CK_TLS_VERIFICATION_ENABLED, null);
    }

    @Test
    @MongoDBFixtures("amqp-tls-verification-inputs.json")
    void otherInputTypesAreNotModified() throws Exception {
        assertThat(inputService.find(OTHER_ID).getConfiguration()).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);
    }

    @Test
    void newInputWithoutSettingIsSavedWithVerificationEnabled() throws Exception {
        final String id = inputService.save(newAmqpInput(Map.of(AmqpTransport.CK_TLS, true)));

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
        assertThat(inputService.find(id).getConfiguration()).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
    }

    @Test
    void newInputWithNullSettingIsSavedWithVerificationEnabled() throws Exception {
        final Map<String, Object> config = new HashMap<>();
        config.put(AmqpTransport.CK_TLS, true);
        config.put(CK_TLS_VERIFICATION_ENABLED, null);

        final String id = inputService.save(newAmqpInput(config));

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
    }

    @Test
    void newInputWithDisabledSettingIsSavedWithVerificationDisabled() throws Exception {
        final String id = inputService.save(newAmqpInput(Map.of(AmqpTransport.CK_TLS, true, CK_TLS_VERIFICATION_ENABLED, false)));

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
    }

    @Test
    void newInputOfOtherTypeIsNotModified() throws Exception {
        final String id = inputService.save(InputImpl.builder()
                .setTitle("other")
                .setType(OTHER_TYPE)
                .setCreatorUserId("admin")
                .setGlobal(true)
                .setCreatedAt(new DateTime(DateTimeZone.UTC))
                .setConfiguration(Map.of("port", 5555))
                .build());

        assertThat(persistedConfiguration(id)).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);
    }

    @Test
    @MongoDBFixtures("amqp-tls-verification-inputs.json")
    void savingExistingInputDoesNotAddSetting() throws Exception {
        final Input existing = inputService.create(LEGACY_ID, Map.of(
                MessageInput.FIELD_TYPE, AMQP_TYPE,
                MessageInput.FIELD_TITLE, "renamed",
                MessageInput.FIELD_CREATOR_USER_ID, "admin",
                MessageInput.FIELD_GLOBAL, true,
                MessageInput.FIELD_CONFIGURATION, persistedConfiguration(LEGACY_ID)
        ));

        inputService.save(existing);
        assertThat(persistedConfiguration(LEGACY_ID)).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);

        inputService.update(existing);
        assertThat(persistedConfiguration(LEGACY_ID)).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);
    }

    private static void assertExpectedVerificationSettings(Iterable<Input> inputs) {
        assertThat(inputs).allSatisfy(input -> {
            final Map<String, Object> config = input.getConfiguration();
            switch (input.getId()) {
                case LEGACY_ID, NULL_ID -> assertThat(config).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
                case ENABLED_ID -> assertThat(config).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
                case OTHER_ID -> assertThat(config).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);
                default -> throw new AssertionError("Unexpected input " + input.getId());
            }
        });
        assertThat(inputs).extracting(Input::getId).contains(LEGACY_ID, NULL_ID, ENABLED_ID);
    }

    private Map<String, Object> persistedConfiguration(String id) {
        final Document document = rawCollection.find(Filters.eq("_id", new ObjectId(id))).first();
        assertThat(document).isNotNull();
        return document.get(MessageInput.FIELD_CONFIGURATION, Document.class);
    }

    private static InputImpl newAmqpInput(Map<String, Object> configuration) {
        return InputImpl.builder()
                .setTitle("new AMQP input")
                .setType(AMQP_TYPE)
                .setCreatorUserId("admin")
                .setGlobal(true)
                .setCreatedAt(new DateTime(DateTimeZone.UTC))
                .setConfiguration(configuration)
                .build();
    }

    static MessageInput.Config amqpInputConfig() {
        return new MessageInput.Config(new AmqpTransport.Config(), new RawCodec.Config()) {};
    }

    private static MessageInput.Config otherInputConfig() {
        return new MessageInput.Config(ConfigurationRequest::new, new RawCodec.Config()) {};
    }
}
