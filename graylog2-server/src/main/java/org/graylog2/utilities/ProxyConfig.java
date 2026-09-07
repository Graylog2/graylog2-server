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
package org.graylog2.utilities;

import com.google.common.base.Splitter;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A configured HTTP proxy, parsed once from {@code http_proxy_uri}.
 *
 * <p>Deliberately narrow: this holds only the proxy's own identity (host, port, scheme, credentials),
 * not {@code http_non_proxy_hosts} -- that bypass list is only meaningful to selector construction
 * ({@code ProxySelectorProvider}), which keeps taking it as its own parameter. This type also builds no
 * client-specific proxy object (no {@code java.net.ProxySelector}, no OkHttp {@code Authenticator}, no
 * AWS {@code ProxyConfiguration}) -- every HTTP client library's proxy API is different enough that
 * adapting this to a specific client belongs in that client's own provider/factory.</p>
 */
public record ProxyConfig(URI uri) {

    /** {@code user:password} parsed from the proxy URI's user info. */
    public record Credentials(String username, String password) {}

    public String host() {
        return uri.getHost();
    }

    public int port() {
        return uri.getPort();
    }

    public int port(int httpDefault, int httpsDefault) {
        final int port = port();
        return port >= 0 ? port : ("https".equalsIgnoreCase(scheme()) ? httpsDefault : httpDefault);
    }

    public String scheme() {
        return uri.getScheme();
    }

    /**
     * The proxy URI with user info stripped. Some HTTP client SDKs (e.g. AWS's Apache5 client) reject a
     * URI that carries credentials in this position and require them supplied separately.
     */
    public URI endpoint() {
        return port() >= 0
                ? URI.create(f("%s://%s:%d", scheme(), host(), port()))
                : URI.create(f("%s://%s", scheme(), host()));
    }

    public Optional<Credentials> credentials() {
        if (isNullOrEmpty(uri.getUserInfo())) {
            return Optional.empty();
        }
        final List<String> userInfo = Splitter.on(':').limit(2).splitToList(uri.getUserInfo());
        return userInfo.size() == 2
                ? Optional.of(new Credentials(userInfo.get(0), userInfo.get(1)))
                : Optional.empty();
    }
}
