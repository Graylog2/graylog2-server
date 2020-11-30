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
package org.graylog2.shared.bindings.providers;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import okhttp3.Authenticator;
import okhttp3.Challenge;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.graylog2.utilities.ProxyHostsPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.net.HttpHeaders.PROXY_AUTHORIZATION;
import static java.util.Objects.requireNonNull;

/**
 * Provider for a configured {@link okhttp3.OkHttpClient}.
 *
 * @see org.graylog2.plugin.BaseConfiguration#getHttpConnectTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpReadTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpWriteTimeout()
 * @see org.graylog2.plugin.BaseConfiguration#getHttpProxyUri()
 */
@Singleton
public class OkHttpClientProvider implements Provider<OkHttpClient> {
    private static final Logger LOG = LoggerFactory.getLogger(OkHttpClientProvider.class);
    protected final Duration connectTimeout;
    protected final Duration readTimeout;
    protected final Duration writeTimeout;
    protected final URI httpProxyUri;
    protected final ProxyHostsPattern nonProxyHostsPattern;

    @Inject
    public OkHttpClientProvider(@Named("http_connect_timeout") Duration connectTimeout,
                                @Named("http_read_timeout") Duration readTimeout,
                                @Named("http_write_timeout") Duration writeTimeout,
                                @Named("http_proxy_uri") @Nullable URI httpProxyUri,
                                @Named("http_non_proxy_hosts") @Nullable ProxyHostsPattern nonProxyHostsPattern) {
        this.connectTimeout = requireNonNull(connectTimeout);
        this.readTimeout = requireNonNull(readTimeout);
        this.writeTimeout = requireNonNull(writeTimeout);
        this.httpProxyUri = httpProxyUri;
        this.nonProxyHostsPattern = nonProxyHostsPattern;
    }

    @Override
    public OkHttpClient get() {
        final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(connectTimeout.getQuantity(), connectTimeout.getUnit())
                .writeTimeout(writeTimeout.getQuantity(), writeTimeout.getUnit())
                .readTimeout(readTimeout.getQuantity(), readTimeout.getUnit());

        if (httpProxyUri != null) {
            final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyUri.getHost(), httpProxyUri.getPort()));
            final ProxySelector proxySelector = new ProxySelector() {
                @Override
                public List<Proxy> select(URI uri) {
                    final String host = uri.getHost();
                    if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(host)) {
                        LOG.debug("Bypassing proxy server for {}", host);
                        return ImmutableList.of(Proxy.NO_PROXY);
                    }
                    try {
                        final InetAddress targetAddress = InetAddress.getByName(host);
                        if (targetAddress.isLoopbackAddress()) {
                            return ImmutableList.of(Proxy.NO_PROXY);
                        } else if (nonProxyHostsPattern != null && nonProxyHostsPattern.matches(targetAddress.getHostAddress())) {
                            LOG.debug("Bypassing proxy server for {}", targetAddress.getHostAddress());
                            return ImmutableList.of(Proxy.NO_PROXY);
                        }
                    } catch (UnknownHostException e) {
                        LOG.debug("Unable to resolve host name for proxy selection: ", e);
                    }
                    return ImmutableList.of(proxy);
                }

                @Override
                public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    LOG.warn("Unable to connect to proxy: ", ioe);
                }
            };

            clientBuilder.proxySelector(proxySelector);

            if (!isNullOrEmpty(httpProxyUri.getUserInfo())) {
                final List<String> list = Splitter.on(":")
                        .limit(2)
                        .splitToList(httpProxyUri.getUserInfo());
                if (list.size() == 2) {
                    clientBuilder.proxyAuthenticator(new ProxyAuthenticator(list.get(0), list.get(1)));
                }
            }
        }

        return clientBuilder.build();
    }

    public static class ProxyAuthenticator implements Authenticator {
        private static final Logger LOG = LoggerFactory.getLogger(ProxyAuthenticator.class);
        private static final String AUTH_BASIC = "basic";

        private final String credentials;

        ProxyAuthenticator(String user, String password) {
            this.credentials = Credentials.basic(requireNonNull(user, "user"), requireNonNull(password, "password"));
        }

        @Nullable
        @Override
        public Request authenticate(@Nonnull Route route, @Nonnull Response response) throws IOException {
            final Set<String> authenticationMethods = response.challenges().stream()
                    .map(Challenge::scheme)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toSet());

            if (!authenticationMethods.contains(AUTH_BASIC)) {
                LOG.warn("Graylog only supports the \"{}\" authentication scheme but the proxy server asks for one of the following: {}",
                        AUTH_BASIC, authenticationMethods);
                return null;
            }

            if (response.request().header(PROXY_AUTHORIZATION) != null) {
                return null; // Give up, we've already failed to authenticate.
            }
            return response.request().newBuilder().addHeader(PROXY_AUTHORIZATION, credentials).build();
        }
    }
}
