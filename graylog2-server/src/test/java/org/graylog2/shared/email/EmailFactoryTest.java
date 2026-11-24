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
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.shared.email.SmtpOAuth2TokenProvider.AccessToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;

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
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmailFactoryTest {
    private static final String AUTH_REJECTED = "535 5.7.3 Authentication unsuccessful";

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
        when(tokenProvider.getAccessToken()).thenReturn(new AccessToken("the-access-token", false));

        send(oauth2EmailFactory());

        assertThat(smtpServer.authCommands()).containsExactly(xoauth2("the-access-token"));
        assertThat(smtpServer.commands()).contains("MAIL FROM:<sender@example.com>", "RCPT TO:<recipient@example.com>");
    }

    @Test
    void requestsTokenWhenSendingNotWhenCreatingEmail() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn(new AccessToken("the-access-token", false));

        final SimpleEmail email = oauth2EmailFactory().simpleEmail();
        verifyNoInteractions(tokenProvider);

        email.addTo("recipient@example.com");
        email.setSubject("Test");
        email.setMsg("Hello");
        email.send();
        verify(tokenProvider).getAccessToken();
    }

    @Test
    void retriesWithNewTokenIfCachedTokenIsRejected() throws Exception {
        smtpServer.rejectNextAuth();
        when(tokenProvider.getAccessToken()).thenReturn(
                new AccessToken("cached-token", true),
                new AccessToken("new-token", false));

        send(oauth2EmailFactory());

        assertThat(smtpServer.authCommands()).containsExactly(xoauth2("cached-token"), xoauth2("new-token"));
        assertThat(smtpServer.commands()).contains("RCPT TO:<recipient@example.com>");
        final var inOrder = inOrder(tokenProvider);
        inOrder.verify(tokenProvider).getAccessToken();
        inOrder.verify(tokenProvider).invalidate("cached-token");
        inOrder.verify(tokenProvider).getAccessToken();
    }

    @Test
    void doesNotRetryIfNewlyRequestedTokenIsRejected() throws Exception {
        smtpServer.rejectNextAuth();
        when(tokenProvider.getAccessToken()).thenReturn(new AccessToken("new-token", false));

        assertThatThrownBy(() -> send(oauth2EmailFactory()))
                .isInstanceOf(EmailException.class)
                .cause()
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage("SMTP server <127.0.0.1> rejected the OAuth 2.0 access token for <graylog@example.com>: " + AUTH_REJECTED);

        assertThat(smtpServer.authCommands()).containsExactly(xoauth2("new-token"));
        verify(tokenProvider, never()).invalidate("new-token");
    }

    @Test
    void retriesOnlyOnce() throws Exception {
        smtpServer.rejectNextAuth();
        smtpServer.rejectNextAuth();
        when(tokenProvider.getAccessToken()).thenReturn(
                new AccessToken("cached-token", true),
                new AccessToken("new-token", false));

        assertThatThrownBy(() -> send(oauth2EmailFactory()))
                .isInstanceOf(EmailException.class)
                .cause()
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(smtpServer.authCommands()).containsExactly(xoauth2("cached-token"), xoauth2("new-token"));
    }

    @Test
    void reportsTokenRequestFailure() throws Exception {
        when(tokenProvider.getAccessToken()).thenThrow(new MessagingException("Failed to obtain OAuth 2.0 access token"));

        assertThatThrownBy(() -> send(oauth2EmailFactory()))
                .isInstanceOf(EmailException.class)
                .cause()
                .hasMessage("Failed to obtain OAuth 2.0 access token");

        assertThat(smtpServer.authCommands()).isEmpty();
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

    private EmailFactory oauth2EmailFactory() throws Exception {
        final EmailConfiguration configuration = SmtpOAuth2TokenProviderTest.oauth2Config(
                HttpUrl.get("https://login.example.com/token"), smtpSettings());
        return new EmailFactory(configuration, tokenProvider);
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

    private static String xoauth2(String token) {
        return "AUTH XOAUTH2 " + Base64.getEncoder().encodeToString(
                ("user=graylog@example.com\u0001auth=Bearer " + token + "\u0001\u0001").getBytes(StandardCharsets.UTF_8));
    }

    private static void send(EmailFactory emailFactory) throws Exception {
        final SimpleEmail email = emailFactory.simpleEmail();
        email.addTo("recipient@example.com");
        email.setSubject("Test");
        email.setMsg("Hello");
        email.send();
    }

    /**
     * Minimal scripted SMTP server that handles connections one after another, advertises LOGIN and XOAUTH2,
     * accepts any credentials unless told to reject them, and records the commands it receives.
     */
    private static class FakeSmtpServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final List<String> commands = new CopyOnWriteArrayList<>();
        private final Queue<String> authResponses = new ConcurrentLinkedQueue<>();

        FakeSmtpServer() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            final Thread thread = new Thread(this::serve, "fake-smtp-server");
            thread.setDaemon(true);
            thread.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        void rejectNextAuth() {
            authResponses.add(AUTH_REJECTED);
        }

        List<String> commands() {
            return commands;
        }

        List<String> authCommands() {
            return commands.stream().filter(c -> c.startsWith("AUTH ")).toList();
        }

        private void serve() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    handle(socket);
                } catch (IOException ignored) {
                    // Connection or server socket closed
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
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
                        final String response = authResponses.poll();
                        reply(out, response != null ? response : "235 2.7.0 Authentication successful");
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
