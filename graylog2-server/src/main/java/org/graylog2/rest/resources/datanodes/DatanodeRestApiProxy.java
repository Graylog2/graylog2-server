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
package org.graylog2.rest.resources.datanodes;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.ssl.SSLContexts;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.security.IndexerJwtAuthTokenProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

@Singleton
public class DatanodeRestApiProxy implements ProxyRequestAdapter {

    private final NodeService<DataNodeDto> nodeService;
    private final CloseableHttpClient httpClient;

    @Inject
    public DatanodeRestApiProxy(IndexerJwtAuthTokenProvider authTokenProvider, NodeService<DataNodeDto> nodeService, X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyManagementException {
        this.nodeService = nodeService;
        httpClient = createHttpClient(authTokenProvider, trustManager);
    }

    private static CloseableHttpClient createHttpClient(IndexerJwtAuthTokenProvider authTokenProvider, X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext sslcontext = SSLContexts.custom().setProtocol("ssl").build();
        sslcontext.init(null, new X509TrustManager[]{trustManager}, new SecureRandom());

        return HttpClientBuilder.create()
                .setSSLContext(sslcontext)
                .addInterceptorFirst((HttpRequestInterceptor) (request, context) -> request.addHeader("Authorization", authTokenProvider.get()))
                .build();
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        final BasicHttpEntityEnclosingRequest httpEntityEnclosingRequest = new BasicHttpEntityEnclosingRequest(
                request.method(),
                wrapWithLeadingSlash(request.path())
        );

        if (hasBody(request)) {
            httpEntityEnclosingRequest.setEntity(new InputStreamEntity(request.body()));
        }

        final HttpHost host = nodeService.allActive().values().stream()
                .filter(DataNodeDto::isLeader)
                .map(DataNodeDto::getRestApiAddress)
                .map(HttpHost::create)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No datanode present"));

        final CloseableHttpResponse response = httpClient.execute(host, httpEntityEnclosingRequest);
        return new ProxyResponse(response.getStatusLine().getStatusCode(), response.getEntity().getContent());
    }

    private boolean hasBody(ProxyRequest request) throws IOException {
        final boolean isGetOrPost = Objects.equals(request.method(), "POST") || Objects.equals(request.method(), "PUT");
        return isGetOrPost && request.body().available() > 0;
    }

    private String wrapWithLeadingSlash(String path) {
        if (!path.startsWith("/")) {
            return "/" + path;
        } else {
            return path;
        }
    }
}
