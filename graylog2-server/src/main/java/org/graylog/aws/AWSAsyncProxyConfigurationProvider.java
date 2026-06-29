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
package org.graylog.aws;

import com.google.common.base.Splitter;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

import java.net.URI;
import java.util.List;

/**
 * Provides a Netty-based async HTTP client builder configured with the optional Graylog HTTP proxy
 * (the {@code http_proxy_uri} server configuration).
 * <p>
 * The synchronous {@link AWSProxyConfigurationProvider} uses the Apache HTTP client, which cannot be used by the
 * AWS async clients (DynamoDB, CloudWatch, Kinesis) that the Kinesis input relies on. The Kinesis Client Library
 * additionally requires HTTP/2, which is only supported by the Netty async client. This provider therefore mirrors
 * the proxy logic of {@link AWSProxyConfigurationProvider}, but produces a {@link NettyNioAsyncHttpClient.Builder}.
 */
@Singleton
public class AWSAsyncProxyConfigurationProvider implements Provider<NettyNioAsyncHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSAsyncProxyConfigurationProvider.class);
    private static final String HTTPS_SCHEME = "https";
    private static final int DEFAULT_HTTP_PORT = 80;
    private static final int DEFAULT_HTTPS_PORT = 443;

    private final URI httpProxyUri;

    @Inject
    public AWSAsyncProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public NettyNioAsyncHttpClient.Builder get() {
        final NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();
        if (httpProxyUri == null) {
            LOG.debug("AWS async proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(httpProxyUri));
        LOG.debug("AWS async proxy enabled: {}:{}", httpProxyUri.getHost(), httpProxyUri.getPort());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        final String scheme = proxyUri.getScheme();
        final int port = proxyUri.getPort();

        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .scheme(scheme)
                .host(proxyUri.getHost())
                // The Netty proxy configuration requires an explicit port. Fall back to the scheme default when the
                // proxy URI does not specify one.
                .port(port >= 0 ? port : (HTTPS_SCHEME.equalsIgnoreCase(scheme) ? DEFAULT_HTTPS_PORT : DEFAULT_HTTP_PORT));

        if (proxyUri.getUserInfo() != null && !proxyUri.getUserInfo().isEmpty()) {
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }
        }

        return proxyConfigBuilder.build();
    }
}
