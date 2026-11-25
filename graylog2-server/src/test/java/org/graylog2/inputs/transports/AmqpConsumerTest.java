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
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.inputs.transports.AmqpTransport.CK_TLS_VERIFICATION_ENABLED;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AmqpConsumerTest {
    private static final String INPUT_IDENTIFIER = "test-amqp-input";

    private final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
    private final CapturingAppender appender = new CapturingAppender();
    private Logger logger;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        logger = context.getLogger(AmqpConsumer.class.getName());
        originalLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.removeAppender(appender);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void missingVerificationSettingDisablesVerification() throws Exception {
        consumer(Map.of()).configureTls(connectionFactory);

        verifyNoVerification();
    }

    @Test
    void nullVerificationSettingDisablesVerification() throws Exception {
        final Map<String, Object> config = new HashMap<>();
        config.put(CK_TLS_VERIFICATION_ENABLED, null);

        consumer(config).configureTls(connectionFactory);

        verifyNoVerification();
    }

    @Test
    void disabledVerificationSettingDisablesVerification() throws Exception {
        consumer(Map.of(CK_TLS_VERIFICATION_ENABLED, false)).configureTls(connectionFactory);

        verifyNoVerification();
    }

    @Test
    void enabledVerificationSettingEnablesVerification() throws Exception {
        consumer(Map.of(CK_TLS_VERIFICATION_ENABLED, true)).configureTls(connectionFactory);

        verify(connectionFactory).useSslProtocol();
        verify(connectionFactory, never()).useTlsWithNoVerification();
        assertThat(appender.events()).noneMatch(event -> event.getLevel() == Level.WARN);
    }

    private void verifyNoVerification() throws Exception {
        verify(connectionFactory).useTlsWithNoVerification();
        verify(connectionFactory, never()).useSslProtocol();
        assertThat(appender.events())
                .filteredOn(event -> event.getLevel() == Level.WARN)
                .singleElement()
                .satisfies(event -> assertThat(event.getMessage().getFormattedMessage())
                        .contains("WITHOUT certificate verification")
                        .contains(INPUT_IDENTIFIER));
    }

    private AmqpConsumer consumer(Map<String, Object> tlsConfig) {
        final Map<String, Object> config = new HashMap<>(Map.of(
                AmqpTransport.CK_HOSTNAME, "localhost",
                AmqpTransport.CK_PORT, 5671,
                AmqpTransport.CK_VHOST, "/",
                AmqpTransport.CK_PREFETCH, 100,
                AmqpTransport.CK_PARALLEL_QUEUES, 1,
                AmqpTransport.CK_QUEUE, "queue",
                AmqpTransport.CK_TLS, true
        ));
        config.putAll(tlsConfig);

        return AmqpConsumerTlsProbe.consumer(config, INPUT_IDENTIFIER);
    }

    private static class CapturingAppender extends AbstractAppender {
        private final List<LogEvent> events = new CopyOnWriteArrayList<>();

        CapturingAppender() {
            super("capturing", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            events.add(event.toImmutable());
        }

        List<LogEvent> events() {
            return events;
        }
    }
}
