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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
public class DatanodeRestApiProxy implements ProxyRequestAdapter {

    private final IndexerJwtAuthTokenProvider authTokenProvider;
    private final NodeService<DataNodeDto> nodeService;
    private final ObjectMapper objectMapper;
    private final DatanodeResolver datanodeResolver;
    private final OkHttpClient httpClient;

    @Inject
    public DatanodeRestApiProxy(IndexerJwtAuthTokenProvider authTokenProvider, NodeService<DataNodeDto> nodeService, ObjectMapper objectMapper, DatanodeResolver datanodeResolver, OkHttpClient okHttpClient, @Named("proxied_requests_default_call_timeout")
    com.github.joschi.jadconfig.util.Duration defaultProxyTimeout) {
        this.authTokenProvider = authTokenProvider;
        this.nodeService = nodeService;
        this.objectMapper = objectMapper;
        this.datanodeResolver = datanodeResolver;
        httpClient = withTimeout(okHttpClient, defaultProxyTimeout);
    }

    @NotNull
    private static OkHttpClient withTimeout(OkHttpClient okHttpClient, com.github.joschi.jadconfig.util.Duration defaultProxyTimeout) {
        final Duration timeout = Duration.ofMillis(defaultProxyTimeout.toMilliseconds());
        return okHttpClient.newBuilder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .callTimeout(timeout)
                .build();
    }

    private ProxyResponse runOnAllNodes(ProxyRequest request) {
        final Map<String, JsonNode> result = nodeService.allActive().values().stream().parallel().collect(Collectors.toMap(NodeDto::getHostname, n -> {
            try {
                final ProxyResponse response = request(new ProxyRequest(request.method(), request.path(), InputStream.nullInputStream(), n.getHostname()));
                return objectMapper.readValue(response.response(), JsonNode.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(objectMapper.writeValueAsBytes(result));
            return new ProxyResponse(200, bais, "application/json");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ProxyResponse request(ProxyRequest request) throws IOException {

        if (Objects.equals(DatanodeResolver.ALL_NODES_KEYWORD, request.hostname())) {
            return runOnAllNodes(request);
        }

        final String host = datanodeResolver.findByHostname(request.hostname())
                .map(DataNodeDto::getRestApiAddress)
                .map(url -> StringUtils.removeEnd(url, "/"))
                .orElseThrow(() -> new IllegalStateException("No datanode found matching name " + request.hostname()));

        final Request.Builder builder = new Request.Builder()
                .url(host + "/" + StringUtils.removeStart(request.path(), "/"))
                .addHeader("Authorization", authTokenProvider.get());

        switch (request.method().toUpperCase(Locale.ROOT)) {
            case "GET" -> builder.get();
            case "DELETE" -> builder.delete();
            case "POST" -> builder.post(getBody(request));
            case "PUT" -> builder.put(getBody(request));
            default -> throw new IllegalArgumentException("Unsupported method " + request.method());
        }

        final Response response = httpClient.newCall(builder.build()).execute();
        return new ProxyResponse(response.code(), response.body().byteStream(), getContentType(response));
    }

    private String getContentType(Response response) {
        return response.header("Content-Type");
    }

    @NotNull
    private static RequestBody getBody(ProxyRequest request) throws IOException {
        return RequestBody.create(IOUtils.toByteArray(request.body()), MediaType.parse("application/json"));
    }
}
