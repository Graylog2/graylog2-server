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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which redirect URIs clients may register and use.
 * <ul>
 *     <li>Loopback {@code http} URIs are always allowed, with any port (RFC 8252 section 7.3)</li>
 *     <li>Private-use URI schemes of native apps are allowed (RFC 8252 section 7.1)</li>
 *     <li>Other {@code http(s)} URIs need to be allowed by an administrator</li>
 * </ul>
 */
public final class RedirectUriPolicy {
    private static final int MAX_LENGTH = 2000;
    private static final Set<String> LOOPBACK_HOSTS = Set.of("127.0.0.1", "localhost", "[::1]");
    private static final Set<String> FORBIDDEN_SCHEMES = Set.of("javascript", "data", "file", "vbscript", "about", "blob", "ftp", "ws", "wss");

    private RedirectUriPolicy() {
    }

    public static boolean isAllowed(String redirectUri, Collection<String> allowedUris) {
        final Optional<URI> parsed = parse(redirectUri);
        if (parsed.isEmpty()) {
            return false;
        }
        final URI uri = parsed.get();
        final String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (isLoopback(uri)) {
            return true;
        }
        if (scheme.equals("http") || scheme.equals("https")) {
            return allowedUris.contains(redirectUri);
        }
        return !FORBIDDEN_SCHEMES.contains(scheme);
    }

    /**
     * Checks a redirect URI of an authorization request against a registered one. Loopback URIs may differ in port.
     */
    public static boolean matches(String registeredUri, String requestedUri) {
        if (registeredUri.equals(requestedUri)) {
            return true;
        }
        final Optional<URI> registered = parse(registeredUri);
        final Optional<URI> requested = parse(requestedUri);
        if (registered.isEmpty() || requested.isEmpty() || !isLoopback(registered.get()) || !isLoopback(requested.get())) {
            return false;
        }
        final URI a = registered.get();
        final URI b = requested.get();
        return a.getHost().equalsIgnoreCase(b.getHost())
                && Objects.equals(a.getRawPath(), b.getRawPath())
                && Objects.equals(a.getRawQuery(), b.getRawQuery());
    }

    private static boolean isLoopback(URI uri) {
        return "http".equalsIgnoreCase(uri.getScheme())
                && uri.getHost() != null
                && LOOPBACK_HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT));
    }

    private static Optional<URI> parse(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank() || redirectUri.length() > MAX_LENGTH) {
            return Optional.empty();
        }
        try {
            final URI uri = new URI(redirectUri);
            if (!uri.isAbsolute() || uri.getRawFragment() != null || uri.getRawUserInfo() != null) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
    }
}
