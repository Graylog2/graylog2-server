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
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.MetricRegistry;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.rabbitmq.client.ConnectionFactory;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog2.Configuration;
import org.graylog2.database.MongoCollections;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.inputs.InputServiceImpl;
import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.inputs.raw.amqp.RawAMQPInput;
import org.graylog2.inputs.transports.AmqpConsumerTlsProbe;
import org.graylog2.inputs.transports.AmqpTransport;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.models.system.inputs.requests.InputCreateRequest;
import org.graylog2.rest.models.system.inputs.responses.InputCreated;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.SuppressForbidden;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.inputs.transports.AmqpTransport.CK_TLS_VERIFICATION_ENABLED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Inputs created before the {@link AmqpTransport#CK_TLS_VERIFICATION_ENABLED} setting existed must keep working
 * without TLS certificate verification, while new inputs verify certificates.
 */
@ExtendWith(MongoDBExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
@WithAuthorization(permissions = {"*"})
class InputsResourceAmqpTlsVerificationTest {
    private static final String AMQP_TYPE = RawAMQPInput.class.getCanonicalName();
    private static final String LEGACY_ID = "6a1e00000000000000000001";

    private RawAMQPInput.Config amqpConfig;
    private InputsResourceTest.InputsTestResource resource;
    private MongoCollection<Document> rawCollection;

    @BeforeEach
    @SuppressForbidden("Executors#newSingleThreadExecutor() is okay for tests")
    void setUp(MongoCollections mongoCollections) {
        final AmqpTransport.Factory transportFactory = mock(AmqpTransport.Factory.class);
        when(transportFactory.getConfig()).thenReturn(new AmqpTransport.Config());
        when(transportFactory.create(any())).thenReturn(mock(AmqpTransport.class));

        final RawCodec.Factory codecFactory = mock(RawCodec.Factory.class);
        when(codecFactory.getConfig()).thenReturn(new RawCodec.Config());
        when(codecFactory.create(any())).thenAnswer(invocation ->
                new RawCodec(invocation.getArgument(0), new TestMessageFactory()));

        amqpConfig = new RawAMQPInput.Config(transportFactory, codecFactory);

        final RawAMQPInput.Factory inputFactory = mock(RawAMQPInput.Factory.class);
        when(inputFactory.getConfig()).thenReturn(amqpConfig);
        when(inputFactory.create(any())).thenAnswer(invocation -> new RawAMQPInput(
                new MetricRegistry(),
                invocation.getArgument(0),
                transportFactory,
                codecFactory,
                new LocalMetricRegistry(),
                amqpConfig,
                new RawAMQPInput.Descriptor(),
                mock(ServerStatus.class)));

        final MessageInputFactory messageInputFactory = new MessageInputFactory(
                Map.of(AMQP_TYPE, inputFactory), mock(FeatureFlags.class));

        final InputServiceImpl inputService = new InputServiceImpl(
                mongoCollections,
                mock(ExtractorFactory.class),
                mock(ConverterFactory.class),
                messageInputFactory,
                new ClusterEventBus("inputs-test", Executors.newSingleThreadExecutor()),
                new ObjectMapperProvider().get());

        resource = new InputsResourceTest.InputsTestResource(
                inputService,
                mock(StreamService.class),
                mock(StreamRuleService.class),
                mock(PipelineService.class),
                messageInputFactory,
                mock(Configuration.class));
        rawCollection = mongoCollections.nonEntityCollection(InputServiceImpl.COLLECTION_NAME, Document.class);
    }

    @Test
    void creatingInputWithWebInterfaceDefaultsEnablesVerification() throws Exception {
        final String id = createInput(webInterfaceDefaults());

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
    }

    @Test
    void creatingInputWithoutSettingEnablesVerification() throws Exception {
        final Map<String, Object> configuration = webInterfaceDefaults();
        configuration.remove(CK_TLS_VERIFICATION_ENABLED);

        final String id = createInput(configuration);

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, true);
    }

    @Test
    void creatingInputWithDisabledSettingDisablesVerification() throws Exception {
        final Map<String, Object> configuration = webInterfaceDefaults();
        configuration.put(CK_TLS_VERIFICATION_ENABLED, false);

        final String id = createInput(configuration);

        assertThat(persistedConfiguration(id)).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
    }

    @Test
    @MongoDBFixtures("org/graylog2/inputs/amqp-tls-verification-inputs.json")
    void existingInputKeepsVerificationDisabledWhenEditedInWebInterface() throws Exception {
        assertThat(persistedConfiguration(LEGACY_ID)).doesNotContainKey(CK_TLS_VERIFICATION_ENABLED);

        // The web interface loads the input and reports the effective setting.
        final Map<String, Object> loadedConfiguration = resource.get(LEGACY_ID).attributes();
        assertThat(loadedConfiguration).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);

        // The edit form fills settings that are missing in the loaded configuration with the field defaults
        // and sends all settings back.
        final Map<String, Object> formValues = webInterfaceDefaults();
        formValues.putAll(loadedConfiguration);
        resource.update(InputCreateRequest.builder()
                .title("renamed")
                .type(AMQP_TYPE)
                .global(true)
                .configuration(formValues)
                .build(), LEGACY_ID);

        final Map<String, Object> persistedConfiguration = persistedConfiguration(LEGACY_ID);
        assertThat(persistedConfiguration).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);
        assertThat(resource.get(LEGACY_ID).attributes()).containsEntry(CK_TLS_VERIFICATION_ENABLED, false);

        // Launching the input uses TLS without certificate verification.
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        AmqpConsumerTlsProbe.configureTls(persistedConfiguration, LEGACY_ID, connectionFactory);
        verify(connectionFactory).useTlsWithNoVerification();
        verify(connectionFactory, never()).useSslProtocol();
    }

    @Test
    @MongoDBFixtures("org/graylog2/inputs/amqp-tls-verification-inputs.json")
    void existingInputWithoutSettingIsLaunchedWithoutVerification() throws Exception {
        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        AmqpConsumerTlsProbe.configureTls(persistedConfiguration(LEGACY_ID), LEGACY_ID, connectionFactory);

        verify(connectionFactory).useTlsWithNoVerification();
        verify(connectionFactory, never()).useSslProtocol();
    }

    @Test
    void newInputIsLaunchedWithVerification() throws Exception {
        final String id = createInput(webInterfaceDefaults());

        final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        AmqpConsumerTlsProbe.configureTls(persistedConfiguration(id), id, connectionFactory);

        verify(connectionFactory).useSslProtocol();
        verify(connectionFactory, never()).useTlsWithNoVerification();
    }

    private String createInput(Map<String, Object> configuration) throws Exception {
        final InputCreateRequest request = InputCreateRequest.builder()
                .title("AMQP input")
                .type(AMQP_TYPE)
                .global(true)
                .configuration(configuration)
                .build();
        final InputCreated created = (InputCreated) resource.create(false, request).getEntity();
        return created.id();
    }

    /**
     * Returns the configuration the web interface sends for a new input: the default value of every field
     * with the required settings filled in.
     */
    private Map<String, Object> webInterfaceDefaults() {
        final Map<String, Object> configuration = new HashMap<>();
        for (ConfigurationField field : amqpConfig.combinedRequestedConfiguration().getFields().values()) {
            configuration.put(field.getName(), field.getDefaultValue());
        }
        // Encrypted values are converted by the REST layer's deserializer, which these tests don't use.
        configuration.remove(AmqpTransport.CK_PASSWORD);
        configuration.put(AmqpTransport.CK_HOSTNAME, "rabbitmq.example.com");
        configuration.put(AmqpTransport.CK_PORT, 5671);
        configuration.put(AmqpTransport.CK_TLS, true);
        return configuration;
    }

    private Map<String, Object> persistedConfiguration(String id) {
        final Document document = rawCollection.find(Filters.eq("_id", new ObjectId(id))).first();
        assertThat(document).isNotNull();
        return new HashMap<>(document.get(MessageInput.FIELD_CONFIGURATION, Document.class));
    }
}
