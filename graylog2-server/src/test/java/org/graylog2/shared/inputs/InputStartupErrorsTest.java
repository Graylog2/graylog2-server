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

import io.netty.channel.unix.Errors.NativeIoException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLException;
import java.net.BindException;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
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

        assertThat(result).contains("4317").contains("0.0.0.0");
    }

    @Test
    void bindExceptionCannotAssignAddress() {
        final var input = mockInputWithAddress("192.168.99.99", 5140);
        final var exception = new MisfireException(
                new BindException("Cannot assign requested address"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("192.168.99.99").contains("5140");
    }

    @Test
    void nativeIoExceptionAddressAlreadyInUse() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        // Netty's native (epoll/kqueue) transport throws NativeIoException, not BindException.
        // Constructing a real one needs the native lib loaded, so mock it; the message format
        // matches what Netty produces for a failed bind() syscall with EADDRINUSE.
        final var nativeError = mock(NativeIoException.class);
        when(nativeError.getMessage()).thenReturn("bind(..) failed with error(-98): Address already in use");
        final var exception = new MisfireException("Failed to start input", nativeError);

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("4317").contains("0.0.0.0").contains("already in use");
    }

    @Test
    void unknownHostException() {
        final var input = mockInputWithAddress("badhost.example", 514);
        final var exception = new MisfireException(
                new UnknownHostException("badhost.example"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("badhost.example");
    }

    @Test
    void sslExceptionWithAddress() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        final var exception = new MisfireException("Failed to start gRPC server",
                new SSLException("no suitable certificate found"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("0.0.0.0").contains("4317")
                .contains("no suitable certificate found");
    }

    @Test
    void certificateExceptionWithAddress() {
        final var input = mockInputWithAddress("0.0.0.0", 4317);
        // A malformed/garbage certificate fails during parsing rather than during SSL context build,
        // so the root cause is a CertificateException (a GeneralSecurityException), not an SSLException.
        // Netty wraps the parse failure, so model it as a nested cause here.
        final var exception = new MisfireException("Failed to start gRPC server",
                new IllegalArgumentException("Input stream does not contain valid certificates.",
                        new CertificateException("found no certificates in input stream")));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("0.0.0.0").contains("4317")
                .contains("TLS configuration error")
                .contains("found no certificates in input stream");
    }

    @Test
    void genericExceptionWithAddress() {
        final var input = mockInputWithAddress("0.0.0.0", 9000);
        final var exception = new MisfireException("Startup error",
                new RuntimeException("Something unexpected"));

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).contains("0.0.0.0").contains("9000")
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
    void unknownHostExceptionWithNullMessage() {
        final var input = mockInputWithAddress("0.0.0.0", 514);
        final var exception = new MisfireException(new UnknownHostException());

        final String result = InputStartupErrors.describeFailure(input, exception);

        assertThat(result).doesNotContain("null");
    }
}
