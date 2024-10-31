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
package org.graylog2.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.graylog2.cluster.Node;
import org.graylog2.security.realm.SessionAuthenticator;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.time.Duration;

public class RemoteInterfaceProvider {
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final Duration defaultProxyTimeout;

    @Inject
    public RemoteInterfaceProvider(ObjectMapper objectMapper,
                                   OkHttpClient okHttpClient,
                                   @Named("proxied_requests_default_call_timeout")
                                   com.github.joschi.jadconfig.util.Duration defaultProxyTimeout
    ) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.defaultProxyTimeout = Duration.ofMillis(defaultProxyTimeout.toMilliseconds());
    }

    public <T> T get(Node node, final String authorizationToken, Class<T> interfaceClass, Duration timeout) {
        final OkHttpClient okHttpClient = this.okHttpClient.newBuilder()
                .writeTimeout(timeout)
                .readTimeout(timeout)
                .callTimeout(timeout)
                .connectTimeout(timeout)
                .addInterceptor(chain -> {
                    final Request original = chain.request();

                    final Request.Builder builder = original.newBuilder()
                            .header(CsrfProtectionFilter.HEADER_NAME, "Graylog Server");

                    if (original.headers(HttpHeaders.ACCEPT).isEmpty()) {
                        builder.header(HttpHeaders.ACCEPT, MediaType.JSON_UTF_8.toString());
                    }

                    if (authorizationToken != null) {
                        builder
                                // forward the authentication information of the current user
                                .header(HttpHeaders.AUTHORIZATION, authorizationToken)
                                // do not extend the users session with proxied requests
                                .header(SessionAuthenticator.X_GRAYLOG_NO_SESSION_EXTENSION, "true");
                    }

                    return chain.proceed(builder.build());
                })
                .build();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(node.getTransportAddress())
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .client(okHttpClient)
                .build();

        return retrofit.create(interfaceClass);
    }

    public <T> T get(Node node, final String authorizationToken, Class<T> interfaceClass) {
        return get(node, authorizationToken, interfaceClass, defaultProxyTimeout);
    }

    public <T> T get(Node node, Class<T> interfaceClass) {
        return get(node, null, interfaceClass);
    }
}
