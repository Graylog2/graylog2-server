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
package org.graylog2.shared.email;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import okhttp3.HttpUrl;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.configuration.EmailConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailFactoryTest {
    private FakeSmtpServer smtpServer;
    private final SmtpOAuth2TokenProvider tokenProvider = mock(SmtpOAuth2TokenProvider.class);

    @BeforeEach
    void setUp() throws IOException {
        smtpServer = new FakeSmtpServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        smtpServer.close();
    }

    @Test
    void authenticatesWithXOAuth2() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("the-access-token");
        final EmailConfiguration configuration = SmtpOAuth2TokenProviderTest.oauth2Config(
                HttpUrl.get("https://login.example.com/token"), smtpSettings());

        send(new EmailFactory(configuration, tokenProvider));

        final String expectedInitialResponse = Base64.getEncoder().encodeToString(
                "user=graylog@example.com\u0001auth=Bearer the-access-token\u0001\u0001".getBytes(StandardCharsets.UTF_8));
        assertThat(smtpServer.authCommands()).containsExactly("AUTH XOAUTH2 " + expectedInitialResponse);
        assertThat(smtpServer.commands()).contains("MAIL FROM:<sender@example.com>", "RCPT TO:<recipient@example.com>");
    }

    @Test
    void passwordAuthenticationDoesNotUseXOAuth2() throws Exception {
        final Map<String, String> settings = new HashMap<>(smtpSettings());
        settings.putAll(Map.of(
                "transport_email_use_auth", "true",
                "transport_email_auth_username", "user",
                "transport_email_auth_password", "secret"
        ));
        final EmailConfiguration configuration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(settings), configuration).process();

        send(new EmailFactory(configuration, tokenProvider));

        assertThat(smtpServer.authCommands()).containsExactly("AUTH LOGIN");
        verifyNoInteractions(tokenProvider);
    }

    private Map<String, String> smtpSettings() {
        return Map.of(
                "transport_email_enabled", "true",
                "transport_email_hostname", "127.0.0.1",
                "transport_email_port", String.valueOf(smtpServer.port()),
                "transport_email_use_tls", "false",
                "transport_email_from_email", "sender@example.com"
        );
    }

    private static void send(EmailFactory emailFactory) throws Exception {
        final SimpleEmail email = emailFactory.simpleEmail();
        email.addTo("recipient@example.com");
        email.setSubject("Test");
        email.setMsg("Hello");
        email.send();
    }

    /**
     * Minimal scripted SMTP server that accepts a single connection, advertises LOGIN and XOAUTH2,
     * accepts any credentials and records the commands it receives.
     */
    private static class FakeSmtpServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final List<String> commands = new CopyOnWriteArrayList<>();

        FakeSmtpServer() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            final Thread thread = new Thread(this::serve, "fake-smtp-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        List<String> commands() {
            return commands;
        }

        List<String> authCommands() {
            return commands.stream().filter(c -> c.startsWith("AUTH ")).toList();
        }

        private void serve() {
            try (Socket socket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))) {
                final OutputStream out = socket.getOutputStream();
                reply(out, "220 localhost ESMTP");
                String line;
                while ((line = in.readLine()) != null) {
                    commands.add(line);
                    final String verb = line.split(" ", 2)[0].toUpperCase(Locale.ROOT);
                    switch (verb) {
                        case "EHLO" -> reply(out, "250-localhost\r\n250 AUTH LOGIN XOAUTH2");
                        case "AUTH" -> {
                            if (line.startsWith("AUTH LOGIN")) {
                                reply(out, "334 VXNlcm5hbWU6");
                                in.readLine();
                                reply(out, "334 UGFzc3dvcmQ6");
                                in.readLine();
                            }
                            reply(out, "235 2.7.0 Authentication successful");
                        }
                        case "DATA" -> {
                            reply(out, "354 Start mail input");
                            while (!".".equals(in.readLine())) {
                                // discard message content
                            }
                            reply(out, "250 OK");
                        }
                        case "QUIT" -> {
                            reply(out, "221 Bye");
                            return;
                        }
                        default -> reply(out, "250 OK");
                    }
                }
            } catch (IOException ignored) {
                // Server socket closed
            }
        }

        private static void reply(OutputStream out, String response) throws IOException {
            out.write((response + "\r\n").getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}
