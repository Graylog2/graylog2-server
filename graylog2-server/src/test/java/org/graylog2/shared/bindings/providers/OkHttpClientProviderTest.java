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
package org.graylog2.shared.bindings.providers;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.net.HttpHeaders;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.Proxy;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore
public class OkHttpClientProviderTest {
    private final MockWebServer server = new MockWebServer();

    @Before
    public void setUp() throws IOException {
        server.start();
    }

    @After
    public void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    public void useProxyOnlyForExternalTargets() {
        final OkHttpClient client = client(server.url("/").uri());
        assertThat(client.proxySelector().select(URI.create("http://127.0.0.1/")))
                .hasSize(1)
                .first()
                .matches(proxy -> proxy.type() == Proxy.Type.DIRECT);
        assertThat(client.proxySelector().select(URI.create("http://www.example.com/")))
                .hasSize(1)
                .first()
                .matches(proxy -> proxy.equals(server.toProxyAddress()));
    }

    @Test
    public void testSuccessfulConnectionWithoutProxy() throws IOException, InterruptedException {
        server.enqueue(successfulMockResponse());

        final Request request = new Request.Builder().url(server.url("/")).get().build();
        final Response response = client(null).newCall(request).execute();
        assertThat(response.isSuccessful()).isTrue();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Test");

        assertThat(server.getRequestCount()).isEqualTo(1);
        final RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/");
    }

    @Test
    @Ignore
    public void testSuccessfulProxyConnectionWithoutAuthentication() throws IOException, InterruptedException {
        server.enqueue(successfulMockResponse());

        final Response response = client(server.url("/").uri()).newCall(request()).execute();
        assertThat(response.isSuccessful()).isTrue();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Test");

        assertThat(server.getRequestCount()).isEqualTo(1);
        final RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/");
        assertThat(recordedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
    }

    @Test
    public void testSuccessfulProxyConnectionWithAuthentication() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse());
        server.enqueue(successfulMockResponse());

        final URI proxyURI = URI.create("http://user:password@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();
        assertThat(response.isSuccessful()).isTrue();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Test");

        assertThat(server.getRequestCount()).isEqualTo(2);
        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();
        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("user", "password"));
    }

    @Test
    public void testSuccessfulProxyConnectionWithAuthenticationMechanismUppercase() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse("BASIC"));
        server.enqueue(successfulMockResponse());

        final URI proxyURI = URI.create("http://user:password@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();
        assertThat(response.isSuccessful()).isTrue();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Test");

        assertThat(server.getRequestCount()).isEqualTo(2);
        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();
        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("user", "password"));
    }

    @Test
    @Ignore
    public void testFailingProxyConnectionWithoutAuthentication() throws IOException, InterruptedException {
        server.enqueue(failedMockResponse());

        final URI proxyURI = URI.create("http://" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();
        assertThat(response.isSuccessful()).isFalse();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Failed");

        assertThat(server.getRequestCount()).isEqualTo(1);
        final RecordedRequest recordedRequest = server.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).isEqualTo("/");
        assertThat(recordedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
    }

    @Test
    public void testFailingProxyConnectionWithAuthentication() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse());
        server.enqueue(failedMockResponse());

        final URI proxyURI = server.url("/").newBuilder().username("user").password("password").build().uri();
        final Response response = client(proxyURI).newCall(request()).execute();
        assertThat(response.isSuccessful()).isFalse();
        final ResponseBody body = response.body();
        assertThat(body).isNotNull();
        assertThat(body.string()).isEqualTo("Failed");

        assertThat(server.getRequestCount()).isEqualTo(2);
        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();
        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("user", "password"));
    }

    @Test
    public void testFailingProxyConnectionWithAuthenticationAndUnsupportedScheme() throws IOException {
        server.enqueue(proxyAuthenticateMockResponse("Bearer"));

        final URI proxyURI = URI.create("http://user:password@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();
        assertThat(response.isSuccessful()).isFalse();
        assertThat(response.code()).isEqualTo(407);

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    public void testFailingProxyConnectionWithAuthenticationAndEmptyUsername() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse());
        server.enqueue(failedAuthMockResponse());

        final URI proxyURI = URI.create("http://:password@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();

        assertThat(server.getRequestCount()).isEqualTo(2);

        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();

        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("", "password"));

        assertThat(response.code()).isEqualTo(401);
    }

    @Test
    public void testFailingProxyConnectionWithAuthenticationAndEmptyPassword() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse());
        server.enqueue(failedAuthMockResponse());

        final URI proxyURI = URI.create("http://user:@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();

        assertThat(server.getRequestCount()).isEqualTo(2);

        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();

        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("user", ""));

        assertThat(response.code()).isEqualTo(401);
    }

    @Test
    public void testFailingProxyConnectionWithAuthenticationAndEmptyUsernamePassword() throws IOException, InterruptedException {
        server.enqueue(proxyAuthenticateMockResponse());
        server.enqueue(failedAuthMockResponse());

        final URI proxyURI = URI.create("http://:@" + server.getHostName() + ":" + server.getPort());
        final Response response = client(proxyURI).newCall(request()).execute();

        assertThat(server.getRequestCount()).isEqualTo(2);

        final RecordedRequest unauthenticatedRequest = server.takeRequest();
        assertThat(unauthenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(unauthenticatedRequest.getPath()).isEqualTo("/");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(unauthenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isNull();

        final RecordedRequest authenticatedRequest = server.takeRequest();
        assertThat(authenticatedRequest.getMethod()).isEqualTo("GET");
        assertThat(authenticatedRequest.getPath()).isEqualTo("/");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.HOST)).isEqualTo("www.example.com");
        assertThat(authenticatedRequest.getHeader(HttpHeaders.PROXY_AUTHORIZATION)).isEqualTo(Credentials.basic("", ""));

        assertThat(response.code()).isEqualTo(401);
    }

    private MockResponse successfulMockResponse() {
        return new MockResponse().setResponseCode(200).setBody("Test");
    }

    private MockResponse failedMockResponse() {
        return new MockResponse().setResponseCode(400).setBody("Failed");
    }

    private MockResponse failedAuthMockResponse() {
        return new MockResponse().setResponseCode(401).setBody("Unauthenticated");
    }

    private MockResponse proxyAuthenticateMockResponse() {
        return proxyAuthenticateMockResponse("Basic");
    }

    private MockResponse proxyAuthenticateMockResponse(String scheme) {
        return new MockResponse().setResponseCode(407).addHeader(HttpHeaders.PROXY_AUTHENTICATE, scheme + " realm=\"test\"");
    }

    private OkHttpClient client(URI proxyURI) {
        final OkHttpClientProvider provider = new OkHttpClientProvider(
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                Duration.milliseconds(100L),
                proxyURI,
                null);

        return provider.get();
    }

    private Request request() {
        return new Request.Builder().url("http://www.example.com/").get().build();
    }
}
