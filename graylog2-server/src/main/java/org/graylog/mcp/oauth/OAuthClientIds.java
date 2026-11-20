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
package org.graylog.mcp.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Issues self-contained client IDs for dynamic client registration, so registering a client does not store anything.
 * A client ID carries the client metadata and an HMAC over it, keyed with a secret derived from {@code password_secret}.
 */
@Singleton
public class OAuthClientIds {
    static final int MAX_LENGTH = 4096;
    private static final String PREFIX = "mcp1.";
    private static final String KEY_LABEL = "graylog-mcp-oauth-client-id";
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final HashFunction hmac;
    private final ObjectMapper objectMapper;

    @Inject
    public OAuthClientIds(Configuration configuration, ObjectMapper objectMapper) {
        this(configuration.getPasswordSecret(), objectMapper);
    }

    OAuthClientIds(String passwordSecret, ObjectMapper objectMapper) {
        final byte[] key = Hashing.hmacSha256(passwordSecret.getBytes(StandardCharsets.UTF_8))
                .hashString(KEY_LABEL, StandardCharsets.UTF_8)
                .asBytes();
        this.hmac = Hashing.hmacSha256(key);
        this.objectMapper = objectMapper;
    }

    // The nonce keeps registrations with identical metadata apart, e.g. the same client on two machines
    private record Payload(@JsonProperty("n") String nonce,
                           @JsonProperty("name") String clientName,
                           @JsonProperty("uri") @Nullable String clientUri,
                           @JsonProperty("redirect_uris") List<String> redirectUris,
                           @JsonProperty("iat") long issuedAt) {}

    /**
     * @return the registered client, or empty if its client ID would exceed {@link #MAX_LENGTH}
     */
    public Optional<OAuthClient> issue(String clientName, @Nullable String clientUri, List<String> redirectUris, Instant now) {
        final Payload payload = new Payload(OAuthTokens.generate(), clientName, clientUri, List.copyOf(redirectUris), now.getEpochSecond());
        final String encodedPayload;
        try {
            encodedPayload = BASE64_URL.encodeToString(objectMapper.writeValueAsBytes(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Couldn't serialize OAuth client", e);
        }
        final String clientId = f("%s%s.%s", PREFIX, encodedPayload, BASE64_URL.encodeToString(sign(encodedPayload)));
        if (clientId.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        return Optional.of(toClient(clientId, payload));
    }

    /**
     * @return the client, or empty if the client ID is malformed or its signature doesn't match
     */
    public Optional<OAuthClient> decode(String clientId) {
        if (clientId.length() > MAX_LENGTH || !clientId.startsWith(PREFIX)) {
            return Optional.empty();
        }
        final String[] parts = clientId.substring(PREFIX.length()).split("\\.", -1);
        if (parts.length != 2) {
            return Optional.empty();
        }
        try {
            if (!MessageDigest.isEqual(sign(parts[0]), BASE64_URL_DECODER.decode(parts[1]))) {
                return Optional.empty();
            }
            final Payload payload = objectMapper.readValue(BASE64_URL_DECODER.decode(parts[0]), Payload.class);
            return Optional.of(toClient(clientId, payload));
        } catch (IllegalArgumentException | IOException e) {
            return Optional.empty();
        }
    }

    private byte[] sign(String encodedPayload) {
        return hmac.hashString(encodedPayload, StandardCharsets.US_ASCII).asBytes();
    }

    private static OAuthClient toClient(String clientId, Payload payload) {
        return new OAuthClient(clientId, payload.clientName(), payload.clientUri(), payload.redirectUris(), Instant.ofEpochSecond(payload.issuedAt()));
    }
}
