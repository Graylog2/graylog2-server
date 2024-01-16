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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Singleton
public class DatanodeRestApiProxy implements ProxyRequestAdapter {

    private final IndexerJwtAuthTokenProvider authTokenProvider;
    private final NodeService<DataNodeDto> nodeService;
    private final OkHttpClient httpClient;

    @Inject
    public DatanodeRestApiProxy(IndexerJwtAuthTokenProvider authTokenProvider, NodeService<DataNodeDto> nodeService, OkHttpClient okHttpClient) throws NoSuchAlgorithmException, KeyManagementException {
        this.authTokenProvider = authTokenProvider;
        this.nodeService = nodeService;
        httpClient = okHttpClient;
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {
        final String host = nodeService.allActive().values().stream()
                .filter(DataNodeDto::isLeader)
                .map(DataNodeDto::getRestApiAddress)
                .findFirst()
                .map(url -> StringUtils.removeEnd(url, "/"))
                .orElseThrow(() -> new IllegalStateException("No datanode present"));

        final Request.Builder builder = new Request.Builder()
                .url(host + "/" + request.path())
                .addHeader("Authorization", authTokenProvider.get());

        switch (request.method().toUpperCase(Locale.ROOT)) {
            case "GET" -> builder.get();
            case "DELETE" -> builder.delete();
            case "POST" -> builder.post(getBody(request));
            case "PUT" -> builder.put(getBody(request));
            default -> throw new IllegalArgumentException("Unsupported method " + request.method());
        }

        final Response response = httpClient.newCall(builder.build()).execute();
        return new ProxyResponse(response.code(), response.body().byteStream());
    }

    @NotNull
    private static RequestBody getBody(ProxyRequest request) throws IOException {
        return RequestBody.create(IOUtils.toByteArray(request.body()), MediaType.parse("application/json"));
    }
}
