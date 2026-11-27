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
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.email.OAuth2ClientAssertionTest.ClientCertificate;
import org.graylog2.shared.email.SmtpOAuth2TokenProvider.AccessToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.mail.MessagingException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmtpOAuth2TokenProviderTest {
    private static final Headers JSON = Headers.of("Content-Type", "application/json");
    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    private final MockWebServer server = new MockWebServer();
    private final Clock clock = mock(Clock.class);
    private SmtpOAuth2TokenProvider tokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        server.start();
        when(clock.instant()).thenReturn(NOW);
        tokenProvider = new SmtpOAuth2TokenProvider(oauth2Config(server.url("/tenant/oauth2/v2.0/token")),
                new OkHttpClient(), new ObjectMapperProvider().get(), clock);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void requestsTokenWithClientCredentialsGrant() throws Exception {
        server.enqueue(tokenResponse("token-1", 3599));

        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-1", false));

        final RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/tenant/oauth2/v2.0/token");
        assertThat(request.getHeaders().get("Content-Type")).startsWith("application/x-www-form-urlencoded");
        assertThat(formParams(request)).containsExactlyInAnyOrderEntriesOf(Map.of(
                "grant_type", "client_credentials",
                "client_id", "the-client-id",
                "client_secret", "the-client-secret",
                "scope", "https://outlook.office365.com/.default"
        ));
    }

    @Test
    void requestsTokenWithClientCertificate(@TempDir Path tempDir) throws Exception {
        OAuth2ClientAssertionTest.registerBouncyCastle();
        final ClientCertificate client = ClientCertificate.write(tempDir);
        server.enqueue(tokenResponse("token-1", 3599));

        assertThat(certificateTokenProvider(client.certificateFile(), client.keyFile()).getAccessToken())
                .isEqualTo(new AccessToken("token-1", false));

        final Map<String, String> params = formParams(server.takeRequest());
        assertThat(params).containsOnlyKeys("grant_type", "client_id", "scope", "client_assertion_type", "client_assertion");
        assertThat(params).containsEntry("grant_type", "client_credentials")
                .containsEntry("client_id", "the-client-id")
                .containsEntry("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
        final Claims claims = Jwts.parser().verifyWith(client.keyPair().publicKey()).build()
                .parseSignedClaims(params.get("client_assertion")).getPayload();
        assertThat(claims.getIssuer()).isEqualTo("the-client-id");
        assertThat(claims.getAudience()).containsExactly(server.url("/tenant/oauth2/v2.0/token").toString());
    }

    @Test
    void logsClientCertificateFailure(@TempDir Path tempDir) throws Exception {
        OAuth2ClientAssertionTest.registerBouncyCastle();
        final ClientCertificate client = ClientCertificate.write(tempDir);
        final Path notAKey = Files.writeString(tempDir.resolve("not-a.key"), "not a key");

        assertThat(warningsOfFailedTokenRequest(certificateTokenProvider(client.certificateFile(), notAKey)))
                .singleElement()
                .satisfies(event -> assertThat(event.getMessage().getFormattedMessage())
                        .startsWith("Failed to read OAuth 2.0 client private key <" + notAKey + ">"));
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void cachesTokenUntilShortlyBeforeExpiry() throws Exception {
        server.enqueue(tokenResponse("token-1", 3600));
        server.enqueue(tokenResponse("token-2", 3600));

        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-1", false));

        when(clock.instant()).thenReturn(NOW.plus(Duration.ofSeconds(3600)).minus(SmtpOAuth2TokenProvider.EXPIRY_MARGIN).minusSeconds(1));
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-1", true));
        assertThat(server.getRequestCount()).isEqualTo(1);

        when(clock.instant()).thenReturn(NOW.plus(Duration.ofSeconds(3600)).minus(SmtpOAuth2TokenProvider.EXPIRY_MARGIN));
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-2", false));
        assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    void doesNotCacheTokenWithoutExpiry() throws Exception {
        server.enqueue(new MockResponse(200, JSON, "{\"access_token\":\"token-1\",\"token_type\":\"Bearer\"}"));
        server.enqueue(new MockResponse(200, JSON, "{\"access_token\":\"token-2\",\"token_type\":\"Bearer\"}"));

        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-1");
        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-2");
    }

    @Test
    void shortLivedTokensAreCachedForHalfTheirLifetime() throws Exception {
        server.enqueue(tokenResponse("token-1", 60));
        server.enqueue(tokenResponse("token-2", 60));

        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-1");
        when(clock.instant()).thenReturn(NOW.plusSeconds(29));
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-1", true));
        when(clock.instant()).thenReturn(NOW.plusSeconds(30));
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-2", false));
    }

    @Test
    void invalidateRemovesCachedToken() throws Exception {
        server.enqueue(tokenResponse("token-1", 3600));
        server.enqueue(tokenResponse("token-2", 3600));

        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-1");
        tokenProvider.invalidate("token-1");
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-2", false));
    }

    @Test
    void invalidateIgnoresOtherTokens() throws Exception {
        server.enqueue(tokenResponse("token-2", 3600));

        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-2");
        tokenProvider.invalidate("token-1");
        assertThat(tokenProvider.getAccessToken()).isEqualTo(new AccessToken("token-2", true));
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void accessTokenToStringDoesNotContainToken() {
        assertThat(new AccessToken("secret-token", true).toString()).doesNotContain("secret-token");
    }

    @Test
    void includesOAuthErrorInException() {
        server.enqueue(new MockResponse(401, JSON,
                "{\"error\":\"invalid_client\",\"error_description\":\"AADSTS7000215: Invalid client secret provided.\"}"));

        assertThatThrownBy(tokenProvider::getAccessToken)
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("HTTP 401")
                .hasMessageContaining("invalid_client: AADSTS7000215: Invalid client secret provided.")
                .hasMessageNotContaining("the-client-secret");
    }

    @Test
    void logsTokenRequestFailure() {
        server.enqueue(new MockResponse(401, JSON,
                "{\"error\":\"invalid_client\",\"error_description\":\"AADSTS7000215: Invalid client secret provided.\"}"));

        assertThat(warningsOfFailedTokenRequest(tokenProvider)).singleElement().satisfies(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getMessage().getFormattedMessage())
                    .contains("HTTP 401 (invalid_client: AADSTS7000215: Invalid client secret provided.)")
                    .doesNotContain("the-client-secret");
        });
    }

    @Test
    void failsOnNonJsonErrorResponse() {
        server.enqueue(new MockResponse(503, Headers.of(), "<html>Service Unavailable</html>"));

        assertThatThrownBy(tokenProvider::getAccessToken)
                .isInstanceOf(MessagingException.class)
                .hasMessageEndingWith("HTTP 503");
    }

    @Test
    void failsOnMissingAccessToken() {
        server.enqueue(new MockResponse(200, JSON, "{\"token_type\":\"Bearer\",\"expires_in\":3599}"));

        assertThatThrownBy(tokenProvider::getAccessToken)
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("does not contain an access token");
    }

    @Test
    void retriesAfterFailure() throws Exception {
        server.enqueue(new MockResponse(500, Headers.of(), ""));
        server.enqueue(tokenResponse("token-1", 3599));

        assertThatThrownBy(tokenProvider::getAccessToken).isInstanceOf(MessagingException.class);
        assertThat(tokenProvider.getAccessToken().value()).isEqualTo("token-1");
    }

    static MockResponse tokenResponse(String accessToken, long expiresIn) {
        return new MockResponse(200, JSON,
                "{\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + ",\"ext_expires_in\":" + expiresIn
                        + ",\"access_token\":\"" + accessToken + "\"}");
    }

    static EmailConfiguration oauth2Config(HttpUrl tokenUrl) throws Exception {
        return oauth2Config(tokenUrl, Map.of());
    }

    static EmailConfiguration oauth2Config(HttpUrl tokenUrl, Map<String, String> additionalSettings) throws Exception {
        final Map<String, String> settings = oauth2Settings(tokenUrl);
        settings.putAll(additionalSettings);
        return emailConfiguration(settings);
    }

    private static Map<String, String> oauth2Settings(HttpUrl tokenUrl) {
        return new HashMap<>(Map.of(
                "transport_email_enabled", "true",
                "transport_email_hostname", "localhost",
                "transport_email_use_auth", "true",
                "transport_email_auth_method", "oauth2",
                "transport_email_auth_username", "graylog@example.com",
                "transport_email_auth_oauth2_token_url", tokenUrl.toString(),
                "transport_email_auth_oauth2_client_id", "the-client-id",
                "transport_email_auth_oauth2_client_secret", "the-client-secret",
                "transport_email_auth_oauth2_scope", "https://outlook.office365.com/.default"
        ));
    }

    private static EmailConfiguration emailConfiguration(Map<String, String> settings) throws Exception {
        final EmailConfiguration configuration = new EmailConfiguration();
        new JadConfig(new InMemoryRepository(settings), configuration).process();
        return configuration;
    }

    private SmtpOAuth2TokenProvider certificateTokenProvider(Path certificateFile, Path keyFile) throws Exception {
        final Map<String, String> settings = oauth2Settings(server.url("/tenant/oauth2/v2.0/token"));
        settings.remove("transport_email_auth_oauth2_client_secret");
        settings.put("transport_email_auth_oauth2_client_certificate_file", certificateFile.toString());
        settings.put("transport_email_auth_oauth2_client_private_key_file", keyFile.toString());
        // The certificate is only valid from now on, so don't use the mocked clock.
        return new SmtpOAuth2TokenProvider(emailConfiguration(settings), new OkHttpClient(), new ObjectMapperProvider().get());
    }

    /**
     * Requests a token, expecting it to fail, and returns the warnings logged by the token provider.
     */
    private static List<LogEvent> warningsOfFailedTokenRequest(SmtpOAuth2TokenProvider provider) {
        final Logger logger = (Logger) LogManager.getLogger(SmtpOAuth2TokenProvider.class);
        final CapturingAppender appender = new CapturingAppender();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(provider::getAccessToken).isInstanceOf(MessagingException.class);
        } finally {
            logger.removeAppender(appender);
            appender.stop();
        }
        return appender.events.stream().filter(event -> event.getLevel() == Level.WARN).toList();
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
    }

    private static Map<String, String> formParams(RecordedRequest request) {
        return Arrays.stream(request.getBody().string(StandardCharsets.UTF_8).split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.toMap(
                        kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8)));
    }
}
