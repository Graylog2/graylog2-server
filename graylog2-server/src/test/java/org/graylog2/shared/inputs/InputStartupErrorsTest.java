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
package org.graylog2.shared.inputs;

import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.net.BindException;
import java.net.UnknownHostException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputStartupErrorsTest {

    private MessageInput mockInputWithAddress(String bindAddress, int port) {
        final var input = mock(MessageInput.class);
        when(input.getConfiguration()).thenReturn(new Configuration(Map.of(
                "bind_address", bindAddress,
                "port", port
        )));
        return input;
    }

    private MessageInput mockInputWithoutAddress() {
        final var input = mock(MessageInput.class);
        when(input.getConfiguration()).thenReturn(new Configuration(Map.of()));
        return input;
    }

    @Test
    void bindExceptionAddressAlreadyInUse() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        final var exception = new MisfireException("Failed to start gRPC server",
                new BindException("Address already in use"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Port 4317 is already in use")
                .contains("0.0.0.0:4317");
    }

    @Test
    void bindExceptionCannotAssignAddress() {
        final var input = mockInputWithAddress("192.168.99.99", 5140);
        final var exception = new MisfireException(
                new BindException("Cannot assign requested address"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Cannot bind to 192.168.99.99:5140")
                .contains("Verify the address is available on this host");
    }

    @Test
    void unknownHostException() {
        final var input = mockInputWithAddress("badhost.example", 514);
        final var exception = new MisfireException(
                new UnknownHostException("badhost.example"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Unknown host")
                .contains("badhost.example");
    }

    @Test
    void sslExceptionWithAddress() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        final var exception = new MisfireException("Failed to start gRPC server",
                new SSLException("no suitable certificate found"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("TLS configuration error")
                .contains("0.0.0.0:4317")
                .contains("no suitable certificate found");
    }

    @Test
    void genericExceptionWithAddress() {
        final var input = mockInputWithAddress("0.0.0.0", 9000);
        final var exception = new MisfireException("Startup error",
                new RuntimeException("Something unexpected"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Startup failed on 0.0.0.0:9000")
                .contains("Something unexpected");
    }

    @Test
    void genericExceptionWithoutAddress() {
        final var input = mockInputWithoutAddress();
        final var exception = new RuntimeException("Connection refused");

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Connection refused");
    }

    @Test
    void deeplyNestedExceptionChain() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        final var exception = new MisfireException(
                new MisfireException("Failed to start gRPC server",
                        new BindException("Address already in use")));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("Port 4317 is already in use")
                .contains("0.0.0.0:4317");
    }

    @Test
    void unknownHostExceptionWithNullMessage() {
        final var input = mockInputWithAddress("0.0.0.0", 514);
        final var exception = new MisfireException(new UnknownHostException());

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).isEqualTo("Unknown host.");
    }

    @Test
    void nullExceptionMessage() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        final var exception = new RuntimeException((String) null);

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).isNotNull().isNotEmpty();
    }
}
