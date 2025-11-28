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
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Utility class for AWS proxy configuration to centralize proxy logic across AWS inputs.
 */
public class AWSProxyUtils {

    /**
     * Builds a ProxyConfiguration from a proxy URI, extracting and setting credentials if present.
     * <p>
     * AWS SDK v2 does not support user credentials in the proxy endpoint URI. If the URI contains
     * user info (username:password@host:port), this method strips it from the endpoint and sets
     * the credentials separately on the ProxyConfiguration.Builder.
     * </p>
     *
     * @param proxyUri the proxy URI, potentially containing user credentials
     * @return a configured ProxyConfiguration
     */
    public static ProxyConfiguration buildProxyConfiguration(URI proxyUri) {
        ProxyConfiguration.Builder proxyConfigBuilder = ProxyConfiguration.builder();

        // Check if proxy URI contains user credentials
        if (!isNullOrEmpty(proxyUri.getUserInfo())) {
            // Extract username and password from user info
            final List<String> credentials = Splitter.on(":")
                    .limit(2)
                    .splitToList(proxyUri.getUserInfo());

            if (credentials.size() == 2) {
                proxyConfigBuilder.username(credentials.get(0));
                proxyConfigBuilder.password(credentials.get(1));
            }

            // Create a clean URI without user info for the endpoint
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
                // If we can't create a clean URI, fall back to the original
                // This will likely fail with the AWS SDK validation error, but preserves existing behavior
                proxyConfigBuilder.endpoint(proxyUri);
            }
        } else {
            // No credentials in URI, use as-is
            proxyConfigBuilder.endpoint(proxyUri);
        }

        return proxyConfigBuilder.build();
    }

    /**
     * Creates an ApacheHttpClient.Builder with proxy configuration if proxyUri is provided.
     *
     * @param proxyUri the proxy URI, potentially containing user credentials
     * @return configured ApacheHttpClient.Builder
     */
    public static ApacheHttpClient.Builder createHttpClientBuilder(@Nullable URI proxyUri) {
        ApacheHttpClient.Builder httpClientBuilder = ApacheHttpClient.builder();
        if (proxyUri != null) {
            httpClientBuilder.proxyConfiguration(buildProxyConfiguration(proxyUri));
        }
        return httpClientBuilder;
    }
}
