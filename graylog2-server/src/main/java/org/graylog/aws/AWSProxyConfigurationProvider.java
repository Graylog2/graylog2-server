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
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@Singleton
public class AWSProxyConfigurationProvider implements Provider<ApacheHttpClient.Builder> {
    private static final Logger LOG = LoggerFactory.getLogger(AWSProxyConfigurationProvider.class);
    private final URI httpProxyUri;

    @Inject
    public AWSProxyConfigurationProvider(@Named("http_proxy_uri") @Nullable URI httpProxyUri) {
        this.httpProxyUri = httpProxyUri;
    }

    @Override
    public ApacheHttpClient.Builder get() {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (httpProxyUri == null) {
            LOG.debug("AWS proxy disabled: http_proxy_uri not set");
            return httpClientBuilder;
        }

        httpClientBuilder.proxyConfiguration(buildProxyConfiguration(httpProxyUri));
        LOG.debug("AWS proxy enabled: {}:{}", httpProxyUri.getHost(), httpProxyUri.getPort());
        return httpClientBuilder;
    }

    static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder();

        if (proxyUri.getUserInfo() != null && !proxyUri.getUserInfo().isEmpty()) {
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }

            try {
                URI cleanProxyUri = new URI(
                        proxyUri.getScheme(),
                        null,
                        proxyUri.getHost(),
                        proxyUri.getPort(),
                        proxyUri.getPath(),
                        proxyUri.getQuery(),
                        proxyUri.getFragment()
                );
                proxyConfigBuilder.endpoint(cleanProxyUri);
            } catch (URISyntaxException e) {
                proxyConfigBuilder.endpoint(proxyUri);
            }
        } else {
            proxyConfigBuilder.endpoint(proxyUri);
        }

        return proxyConfigBuilder.build();
    }
}
