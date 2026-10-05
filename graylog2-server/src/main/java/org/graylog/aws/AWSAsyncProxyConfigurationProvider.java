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

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.graylog2.utilities.ProxyConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.ProxyConfiguration;

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

    private final ProxyConfig proxyConfig;

    @Inject
    public AWSAsyncProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public NettyNioAsyncHttpClient.Builder get() {
        final NettyNioAsyncHttpClient.Builder httpClientBuilder = NettyNioAsyncHttpClient.builder();
        if (proxyConfig == null) {
            LOG.debug("AWS async proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyConfig));
        LOG.debug("AWS async proxy enabled: {}:{}", proxyConfig.host(), proxyConfig.port());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .scheme(proxyConfig.scheme())
                .host(proxyConfig.host())
                // The Netty proxy configuration requires an explicit port, which ProxyConfig#port() guarantees.
                .port(proxyConfig.port());

        proxyConfig.credentials().ifPresent(credentials ->
                proxyConfigBuilder.username(credentials.username()).password(credentials.password()));

        return proxyConfigBuilder.build();
    }
}
