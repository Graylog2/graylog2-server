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
                .isInstanceOf(RequestNotAllowedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%2e%2e/secrets", "@evil.example.com/x", "//evil.example.com/x", "http://evil.example.com/x"})
    void staysOnDatanodeHost(String path) {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request(path));

        assertThat(url.host()).isEqualTo("datanode1");
        assertThat(url.port()).isEqualTo(8999);
        assertThat(url.encodedPath()).startsWith("/api/");
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://datanode1:8999", "https://datanode1:8999/"})
    void worksWithRootBaseUrl(String baseUrl) {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl(baseUrl, request("metrics"));

        assertThat(url.toString()).isEqualTo("https://datanode1:8999/metrics");
    }

    /**
     * Characters that would end the path, start a query/fragment, inject userinfo or break the request line are
     * percent-encoded into the path segments; encoded traversal sequences are not decoded into real ".." segments.
     * Note that JAX-RS decodes the path parameter once before it reaches the proxy, so "%252e" arrives as "%2e".
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "_cat?x=1",
            "_cat#fragment",
            "a#@evil.example.com/x",
            "a?@evil.example.com/x",
            "a\r\nHost: evil.example.com",
            "a\tb c",
            ".%2e/secrets",
            "%2e./secrets",
            "%2e%2e%2fsecrets",
            "%252e%252e/secrets",
            "..%2fsecrets",
            "..%5csecrets",
            "\\\\evil.example.com\\x"
    })
    void encodesSpecialCharactersInPath(String path) {
        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request(path));

        assertThat(url.scheme()).isEqualTo("https");
        assertThat(url.host()).isEqualTo("datanode1");
        assertThat(url.port()).isEqualTo(8999);
        assertThat(url.username()).isEmpty();
        assertThat(url.encodedPath()).startsWith("/api/");
        assertThat(url.pathSegments()).doesNotContain("..", ".");
        assertThat(url.query()).isNull();
        assertThat(url.fragment()).isNull();
    }

    @Test
    void encodesQueryParameterKeysAndValues() {
        final MultivaluedMap<String, String> query = new MultivaluedHashMap<>();
        query.add("a&b=c", "1#@evil.example.com/x");
        query.add("d", "e&f=g?h");

        final HttpUrl url = DatanodeRestApiProxy.buildTargetUrl("https://datanode1:8999/api", request("_cat/indices", query));

        assertThat(url.host()).isEqualTo("datanode1");
        assertThat(url.encodedPath()).isEqualTo("/api/_cat/indices");
        assertThat(url.fragment()).isNull();
        assertThat(url.queryParameterNames()).containsExactlyInAnyOrder("a&b=c", "d");
        assertThat(url.queryParameter("a&b=c")).isEqualTo("1#@evil.example.com/x");
        assertThat(url.queryParameter("d")).isEqualTo("e&f=g?h");
    }

    @Test
    void rejectsInvalidBaseUrl() {
        assertThatThrownBy(() -> DatanodeRestApiProxy.buildTargetUrl("not a url", request("_cat")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid REST API address");
    }
}
