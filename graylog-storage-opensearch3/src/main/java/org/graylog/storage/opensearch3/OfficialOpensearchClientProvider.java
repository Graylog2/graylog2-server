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
package org.graylog.storage.opensearch3;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import jakarta.annotation.Nonnull;
import jakarta.inject.Provider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.reactor.ssl.TlsDetails;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.opensearch.client.opensearch.OpenSearchAsyncClient;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.graylog2.configuration.IndexerHosts;
import org.graylog2.security.jwt.IndexerJwtAuthToken;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

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
public class OfficialOpensearchClientProvider implements Provider<OfficialOpensearchClient> {


    private final Supplier<OfficialOpensearchClient> clientCache;

    public OfficialOpensearchClientProvider(@IndexerHosts List<URI> hosts, IndexerJwtAuthToken indexerJwtAuthToken) {
        clientCache = Suppliers.memoize(() -> createClient(hosts, indexerJwtAuthToken));
    }

    @Override
    public OfficialOpensearchClient get() {
        return clientCache.get();
    }

    @Nonnull
    private static OfficialOpensearchClient createClient(List<URI> uris, IndexerJwtAuthToken indexerJwtAuthToken) {

        final HttpHost[] hosts = uris.stream().map(uri -> new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort())).toArray(HttpHost[]::new);

        //  final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        // Only for demo purposes. Don't specify your credentials in code.
        //  credentialsProvider.setCredentials(new AuthScope(host), new UsernamePasswordCredentials("admin", "admin".toCharArray()));

        final SSLContext sslcontext;
        try {
            sslcontext = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(null, (chains, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }

        final ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(hosts);
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            final TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create()
                    .setSslContext(sslcontext)
                    // See https://issues.apache.org/jira/browse/HTTPCLIENT-2219
                    .setTlsDetailsFactory(sslEngine -> new TlsDetails(sslEngine.getSession(), sslEngine.getApplicationProtocol()))
                    .build();

            final PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                    .create()
                    .setTlsStrategy(tlsStrategy)
                    .build();

            if (indexerJwtAuthToken.isJwtAuthEnabled()) {
                httpClientBuilder.addRequestInterceptorLast(jwtInterceptor(indexerJwtAuthToken));
            }

            return httpClientBuilder
                    //.setDefaultCredentialsProvider(credentialsProvider)
                    .setConnectionManager(connectionManager);
        });

        final OpenSearchTransport transport = builder.build();
        return new OfficialOpensearchClient(new OpenSearchClient(transport), new OpenSearchAsyncClient(transport));
    }

    private static HttpRequestInterceptor jwtInterceptor(IndexerJwtAuthToken indexerJwtAuthToken) {
        return (request, entity, context) -> indexerJwtAuthToken.headerValue().ifPresent(value -> request.addHeader("Authorization", value));
    }
}
