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
package org.graylog2.storage.versionprobe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.graylog2.plugin.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

public class VersionProbe {
    private static final Logger LOG = LoggerFactory.getLogger(VersionProbe.class);
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;

    @Inject
    public VersionProbe(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
    }

    public Optional<Version> probe(Collection<URI> hosts) {
        return hosts
                .stream()
                .map(this::probe)
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    public Optional<Version> probe(URI host) {
        final Retrofit retrofit;
        try {
            retrofit = new Retrofit.Builder()
                    .baseUrl(host.toURL())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .client(addAuthenticationIfPresent(host, okHttpClient))
                    .build();
        } catch (MalformedURLException e) {
            LOG.error("Elasticsearch node URL is invalid: " + host.toString(), e);
            return Optional.empty();
        }

        final RootRoute root = retrofit.create(RootRoute.class);

        return rootResponse(root)
                .map(RootResponse::version)
                .map(VersionResponse::number)
                .flatMap(this::parseVersion);
    }

    private OkHttpClient addAuthenticationIfPresent(URI host, OkHttpClient okHttpClient) {
        if (Strings.emptyToNull(host.getUserInfo()) != null) {
            final String[] credentials = host.getUserInfo().split(":");
            final String username = credentials[0];
            final String password = credentials[1];
            final String authToken = Credentials.basic(username, password);

            return okHttpClient.newBuilder()
                    .addInterceptor(chain -> {
                        final Request originalRequest = chain.request();
                        final Request.Builder builder = originalRequest.newBuilder().header("Authorization", authToken);
                        final Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    })
                    .build();
        }

        return okHttpClient;
    }

    private Optional<Version> parseVersion(String versionString) {
        try {
            final com.github.zafarkhaja.semver.Version version = com.github.zafarkhaja.semver.Version.valueOf(versionString);
            return Optional.of(new Version(version));
        } catch (Exception e) {
            LOG.error("Unable to parse version retrieved from Elasticsearch node: <{}>", versionString, e);
            return Optional.empty();
        }
    }

    private Optional<RootResponse> rootResponse(RootRoute rootRoute) {
        try {
            final Response<RootResponse> response = rootRoute.root().execute();
            if (response.isSuccessful()) {
                return Optional.ofNullable(response.body());
            }
        } catch (IOException e) {
            LOG.error("Unable to retrieve version from Elasticsearch node: ", e);
        }
        return Optional.empty();
    }
}
