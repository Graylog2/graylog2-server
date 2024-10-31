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
package org.graylog.integrations.aws.transports;

import com.google.common.eventbus.EventBus;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.codecs.AWSCodec;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.transports.Transport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AWSTransportTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    AWSTransport awsTransport;
    @Mock
    private EventBus serverEventBus;
    @Mock
    private LocalMetricRegistry localRegistry;
    @Mock
    private KinesisTransport mockKinesisCodec;
    @Mock
    private MessageInput messageInput;
    @Mock
    InputFailureRecorder inputFailureRecorder;

    @Before
    public void setUp() throws Exception {

        Map<String, Transport.Factory<? extends Transport>> availableCodecs = new HashMap<>();

        availableCodecs.put(KinesisTransport.NAME, new KinesisTransport.Factory() {

            @Override
            public KinesisTransport create(Configuration configuration) {
                return mockKinesisCodec;
            }

            @Override
            public KinesisTransport.Config getConfig() {
                return null;
            }
        });

        final HashMap<String, Object> configMap = new HashMap<>();
        configMap.put(AWSCodec.CK_AWS_MESSAGE_TYPE, AWSMessageType.KINESIS_CLOUDWATCH_FLOW_LOGS.toString());
        final Configuration configuration = new Configuration(configMap);
        awsTransport = new AWSTransport(configuration, serverEventBus, localRegistry, availableCodecs);
    }

    @Test
    public void testStartStop() throws MisfireException {

        // Start the AWSTransport, and verify that the start method is called on the KinesisTransport.
        awsTransport.launch(messageInput, inputFailureRecorder);
        verify(mockKinesisCodec, times(1)).launch(isA(MessageInput.class), eq(inputFailureRecorder));

        // Verify the same for the stop method.
        awsTransport.stop();
        verify(mockKinesisCodec, times(1)).stop();
    }
}
