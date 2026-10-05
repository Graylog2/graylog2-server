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

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import okhttp3.HttpUrl;
import org.graylog2.indexer.datanode.ProxyRequestAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatanodeRestApiProxyTargetUrlTest {

    private static ProxyRequestAdapter.ProxyRequest request(String path, MultivaluedMap<String, String> queryParameters) {
        return new ProxyRequestAdapter.ProxyRequest("GET", path, InputStream.nullInputStream(), "datanode1", queryParameters);
    }

    private static ProxyRequestAdapter.ProxyRequest request(String path) {
        return request(path, new MultivaluedHashMap<>());
    }

    @Test
    void appendsPathAndQueryToBaseUrl() {
        final MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("format", "json");
        query.add("h", "index,health");

        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999", request("/_cat/indices", query));

        assertThat(url.toString()).isEqualTo("https://datanode1:8999/_cat/indices?format=json&h=index%2Chealth");
    }

    @Test
    void normalizesDotSegments() {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999", request("logs/./x/../stdout"));

        assertThat(url.toString()).isEqualTo("https://datanode1:8999/logs/stdout");
    }

    @Test
    void keepsBasePathPrefix() {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request("metrics"));

        assertThat(url.toString()).isEqualTo("https://datanode1:8999/api/metrics");
    }

    @ParameterizedTest
    @ValueSource(strings = {"../secrets", "logs/../../secrets", "..\\secrets", "/../secrets"})
    void rejectsTraversalAboveRoot(String path) {
        assertThatThrownBy(() -> DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request(path)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%2e%2e/secrets", "@evil.example.com/x", "//evil.example.com/x", "http://evil.example.com/x"})
    void staysOnDatanodeHost(String path) {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request(path));

        assertThat(url.host()).isEqualTo("datanode1");
        assertThat(url.port()).isEqualTo(8999);
        assertThat(url.encodedPath()).startsWith("/api/");
    }

    @Test
    void rejectsInvalidBaseUrl() {
        assertThatThrownBy(() -> DatanodeRestApiProxy.buildTargetUrl("not a url", request("_cat")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid REST API address");
    }
}
