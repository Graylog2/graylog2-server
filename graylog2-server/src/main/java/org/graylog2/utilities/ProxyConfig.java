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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Objects;
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

    private static final String HTTPS_SCHEME = "https";

    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    public ProxyConfig {
        Objects.requireNonNull(uri, "uri");
    }

    /** {@code user:password} parsed from the proxy URI's user info. */
    public record Credentials(String username, String password) {}

    public String host() {
        return uri.getHost();
    }

    /**
     * The proxy port, falling back to the scheme default when the configured URI does not name one.
     * Always a usable port -- never {@link URI#getPort()}'s {@code -1}, which no consumer of this type
     * can do anything with. Use {@link #uri()}{@code .getPort()} for the raw, possibly absent value.
     */
    public int port() {
        final int port = uri.getPort();
        if (port >= 0) {
            return port;
        }
        return HTTPS_SCHEME.equalsIgnoreCase(scheme()) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT;
    }

    public String scheme() {
        return uri.getScheme();
    }

    /**
     * The proxy's {@code scheme://host:port}, rebuilt from those three components alone: user info is
     * dropped, and so are any path, query and fragment the configured URI carried. The port is always
     * explicit, defaulted from the scheme by {@link #port()} when the configured URI omits it.
     *
     * <p>Some HTTP client SDKs reject a URI that carries credentials in this position and require them
     * supplied separately -- e.g. the Apache (v4) client
     * ({@code software.amazon.awssdk.http.apache.ApacheHttpClient}) used by this repo's
     * {@code AWSProxyConfigurationProvider}, which is also why the remaining components are dropped:
     * it reads only scheme, host and port from the endpoint.</p>
     */
    public URI endpoint() {
        return URI.create(f("%s://%s:%d", scheme(), host(), port()));
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

    public InetSocketAddress getProxyAddress() {
        return new InetSocketAddress(host(), port());
    }
}
