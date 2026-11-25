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
package org.graylog2.inputs.transports;

import com.rabbitmq.client.ConnectionFactory;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.SimpleNodeId;
import org.graylog2.security.encryption.EncryptedValueService;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Applies the TLS setup of an {@link AmqpConsumer} for the given input configuration to a connection factory,
 * the same way the consumer does when an input is launched.
 */
public final class AmqpConsumerTlsProbe {
    private AmqpConsumerTlsProbe() {
    }

    public static void configureTls(Map<String, Object> inputConfiguration, String inputIdentifier, ConnectionFactory factory) throws IOException {
        consumer(inputConfiguration, inputIdentifier).configureTls(factory);
    }

    static AmqpConsumer consumer(Map<String, Object> inputConfiguration, String inputIdentifier) {
        final MessageInput input = mock(MessageInput.class);
        when(input.toIdentifier()).thenReturn(inputIdentifier);

        return new AmqpConsumer(
                60,
                new SimpleNodeId("node-1"),
                input,
                new Configuration(inputConfiguration),
                mock(InputFailureRecorder.class),
                mock(AmqpTransport.class),
                new EncryptedValueService(UUID.randomUUID().toString()),
                Duration.ofSeconds(5)
        );
    }
}
