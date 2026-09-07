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
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

@Singleton
public class AWSProxyConfigurationProvider implements Provider<ApacheHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSProxyConfigurationProvider.class);
    private final ProxyConfig proxyConfig;

    @Inject
    public AWSProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    @Override
    public ApacheHttpClient.Builder get() {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (proxyConfig == null) {
            LOG.debug("AWS proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyConfig));
        LOG.debug("AWS proxy enabled: {}:{}", proxyConfig.host(), proxyConfig.port());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(ProxyConfig proxyConfig) {
        final ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder()
                .endpoint(proxyConfig.endpoint());
        proxyConfig.credentials().ifPresent(credentials ->
                proxyConfigBuilder.username(credentials.username()).password(credentials.password()));
        return proxyConfigBuilder.build();
    }
}
