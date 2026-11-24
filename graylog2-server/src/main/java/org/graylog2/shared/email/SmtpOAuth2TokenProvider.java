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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.graylog2.configuration.EmailConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Obtains OAuth 2.0 access tokens for SMTP authentication (SASL XOAUTH2) using the client credentials grant
 * (RFC 6749, section 4.4) and caches them until shortly before they expire.
 * <p>
 * Tokens are requested by {@link OAuth2SmtpTransport} right before authenticating, so the remaining lifetime of a
 * token only has to cover the SMTP handshake. A cached token that the SMTP server rejects anyway (e.g. because it was
 * revoked) is {@link #invalidate(String) invalidated} and the authentication is retried once with a new token.
 */
@Singleton
public class SmtpOAuth2TokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SmtpOAuth2TokenProvider.class);

    /**
     * Tokens are refreshed this long before they expire. This only needs to cover the time between handing out a
     * token and the SMTP server validating it, plus clock skew. Rejected tokens are handled by a retry, not by this.
     */
    @VisibleForTesting
    static final Duration EXPIRY_MARGIN = Duration.ofMinutes(1);

    private final EmailConfiguration configuration;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private CachedToken cachedToken;

    @Inject
    public SmtpOAuth2TokenProvider(EmailConfiguration configuration, OkHttpClient httpClient, ObjectMapper objectMapper) {
        this(configuration, httpClient, objectMapper, Clock.systemUTC());
    }

    @VisibleForTesting
    SmtpOAuth2TokenProvider(EmailConfiguration configuration, OkHttpClient httpClient, ObjectMapper objectMapper, Clock clock) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.httpClient = requireNonNull(httpClient, "httpClient");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.clock = requireNonNull(clock, "clock");
    }

    /**
     * Returns a valid access token, requesting a new one from the configured token endpoint if necessary.
     *
     * @throws MessagingException if no access token could be obtained.
     */
    public synchronized AccessToken getAccessToken() throws MessagingException {
        final Instant now = clock.instant();
        if (cachedToken != null && now.isBefore(cachedToken.refreshAt())) {
            return new AccessToken(cachedToken.accessToken(), true);
        }

        final TokenResponse tokenResponse = requestToken();
        if (tokenResponse.expiresIn() != null && tokenResponse.expiresIn() > 0) {
            final Instant expiresAt = now.plusSeconds(tokenResponse.expiresIn());
            final Duration margin = Duration.ofSeconds(Math.min(EXPIRY_MARGIN.toSeconds(), tokenResponse.expiresIn() / 2));
            cachedToken = new CachedToken(tokenResponse.accessToken(), expiresAt.minus(margin));
        } else {
            // Without an expiry we cannot know how long the token is valid, so don't cache it.
            cachedToken = null;
        }
        return new AccessToken(tokenResponse.accessToken(), false);
    }

    /**
     * Removes the given token from the cache, so the next call to {@link #getAccessToken()} requests a new one.
     * Does nothing if a different token has been cached in the meantime.
     */
    public synchronized void invalidate(String accessToken) {
        if (cachedToken != null && cachedToken.accessToken().equals(accessToken)) {
            cachedToken = null;
        }
    }

    private TokenResponse requestToken() throws MessagingException {
        final HttpUrl tokenUrl = HttpUrl.get(configuration.getOAuth2TokenUrl().toString());
        final Request request = new Request.Builder()
                .url(tokenUrl)
                .header("Accept", "application/json")
                .post(new FormBody.Builder()
                        .add("grant_type", "client_credentials")
                        .add("client_id", configuration.getOAuth2ClientId())
                        .add("client_secret", configuration.getOAuth2ClientSecret())
                        .add("scope", configuration.getOAuth2Scope())
                        .build())
                .build();

        LOG.debug("Requesting OAuth 2.0 access token for SMTP authentication from <{}>", tokenUrl);
        try (Response response = httpClient.newCall(request).execute()) {
            final String body = response.body().string();
            if (!response.isSuccessful()) {
                throw logged(new MessagingException(f("Failed to obtain OAuth 2.0 access token for SMTP authentication from <%s>: HTTP %d%s",
                        tokenUrl, response.code(), errorDetails(body))));
            }

            final TokenResponse tokenResponse = objectMapper.readValue(body, TokenResponse.class);
            if (Strings.isNullOrEmpty(tokenResponse.accessToken())) {
                throw logged(new MessagingException(f("OAuth 2.0 token response from <%s> does not contain an access token", tokenUrl)));
            }
            return tokenResponse;
        } catch (IOException e) {
            throw logged(new MessagingException(f("Failed to obtain OAuth 2.0 access token for SMTP authentication from <%s>: %s",
                    tokenUrl, e.getMessage()), e));
        }
    }

    private static MessagingException logged(MessagingException e) {
        if (e.getCause() != null) {
            LOG.warn(e.getMessage(), e.getCause());
        } else {
            LOG.warn(e.getMessage());
        }
        return e;
    }

    private String errorDetails(String body) {
        try {
            final TokenErrorResponse error = objectMapper.readValue(body, TokenErrorResponse.class);
            if (!Strings.isNullOrEmpty(error.error())) {
                return Strings.isNullOrEmpty(error.errorDescription())
                        ? f(" (%s)", error.error())
                        : f(" (%s: %s)", error.error(), error.errorDescription());
            }
        } catch (IOException ignored) {
            // Not a standard OAuth 2.0 error response.
        }
        return "";
    }

    /**
     * @param value     the access token
     * @param fromCache whether the token was cached from an earlier request rather than requested just now
     */
    public record AccessToken(String value, boolean fromCache) {
        @Override
        public String toString() {
            return f("AccessToken{fromCache=%s}", fromCache);
        }
    }

    private record CachedToken(String accessToken, Instant refreshAt) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("token_type") @Nullable String tokenType,
                         @JsonProperty("expires_in") @Nullable Long expiresIn) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenErrorResponse(@JsonProperty("error") @Nullable String error,
                              @JsonProperty("error_description") @Nullable String errorDescription) {}
}
