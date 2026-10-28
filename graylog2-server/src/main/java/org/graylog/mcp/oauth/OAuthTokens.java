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

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

public final class OAuthTokens {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    // RFC 7636 section 4.1 and 4.2
    private static final Pattern PKCE_PATTERN = Pattern.compile("^[A-Za-z0-9\\-._~]{43,128}$");

    private OAuthTokens() {
    }

    public static String generate() {
        final byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }

    public static String hash(String token) {
        return Hashing.sha256().hashString(token, StandardCharsets.UTF_8).toString();
    }

    public static boolean isValidCodeChallenge(String codeChallenge) {
        return PKCE_PATTERN.matcher(codeChallenge).matches();
    }

    /**
     * Verifies an RFC 7636 code verifier against an {@code S256} code challenge.
     */
    public static boolean verifyPkce(String codeVerifier, String codeChallenge) {
        if (!PKCE_PATTERN.matcher(codeVerifier).matches()) {
            return false;
        }
        final byte[] digest = Hashing.sha256().hashString(codeVerifier, StandardCharsets.US_ASCII).asBytes();
        final byte[] expected = BASE64_URL.encodeToString(digest).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, codeChallenge.getBytes(StandardCharsets.US_ASCII));
    }
}
