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
package org.graylog.datanode.management;

import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.security.certutil.CaService;
import org.graylog.shaded.opensearch2.org.apache.http.HttpHost;
import org.graylog.shaded.opensearch2.org.apache.http.auth.AuthScope;
import org.graylog.shaded.opensearch2.org.apache.http.auth.UsernamePasswordCredentials;
import org.graylog.shaded.opensearch2.org.apache.http.client.CredentialsProvider;
import org.graylog.shaded.opensearch2.org.apache.http.impl.client.BasicCredentialsProvider;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.security.CustomCAX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class OpensearchRestClient {
    private static final Logger LOG = LoggerFactory.getLogger(OpensearchRestClient.class);

    public static RestHighLevelClient build(final OpensearchConfiguration configuration, final CaService caService) {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        final HttpHost host = configuration.getRestBaseUrl();

        org.graylog.shaded.opensearch2.org.opensearch.client.RestClientBuilder builder = org.graylog.shaded.opensearch2.org.opensearch.client.RestClient.builder(host);
        if ("https".equals(host.getSchemeName())) {
            if (configuration.authUsername() != null && configuration.authPassword() != null) {
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(configuration.authUsername(), configuration.authPassword()));
            }

            try {
                final var sslContext = SSLContext.getInstance("TLS");
                final var tm = new CustomCAX509TrustManager(caService);
                sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());

                builder.setHttpClientConfigCallback(httpClientBuilder -> {
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    httpClientBuilder.setSSLContext(sslContext);
                    return httpClientBuilder;
                });
            } catch (NoSuchAlgorithmException | KeyManagementException ex) {
                LOG.error("Could not initialize SSL correctly: {}", ex.getMessage(), ex);
            }
        }
        return new RestHighLevelClient(builder);
    }
}
