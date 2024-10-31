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
import com.github.joschi.jadconfig.util.Duration;
import com.github.rholder.retry.Attempt;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.RetryListener;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.github.zafarkhaja.semver.Version;
import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.IndexerJwtAuthTokenProvider;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class VersionProbe {
    private static final Logger LOG = LoggerFactory.getLogger(VersionProbe.class);
    private final VersionProbeListener loggingListener = new VersionProbeLogger(LOG);
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final int connectionAttempts;
    private final Duration delayBetweenAttempts;
    private final boolean isJwtAuthentication;
    private final IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider;

    @Inject
    public VersionProbe(ObjectMapper objectMapper,
                        OkHttpClient okHttpClient,
                        @Named("elasticsearch_version_probe_attempts") int elasticsearchVersionProbeAttempts,
                        @Named("elasticsearch_version_probe_delay") Duration elasticsearchVersionProbeDelay,
                        @RunsWithDataNode Boolean runsWithDataNode,
                        @Named("indexer_use_jwt_authentication") boolean opensearchUseJwtAuthentication,
                        IndexerJwtAuthTokenProvider indexerJwtAuthTokenProvider) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.connectionAttempts = elasticsearchVersionProbeAttempts;
        this.delayBetweenAttempts = elasticsearchVersionProbeDelay;
        this.isJwtAuthentication = runsWithDataNode || opensearchUseJwtAuthentication;
        this.indexerJwtAuthTokenProvider = indexerJwtAuthTokenProvider;
    }

    public Optional<SearchVersion> probe(final Collection<URI> hosts) {
        return probe(hosts, this.loggingListener);
    }

    public Optional<SearchVersion> probe(final Collection<URI> hosts, VersionProbeListener probeListener) {
        try {
            return RetryerBuilder.<Optional<SearchVersion>>newBuilder()
                    .retryIfResult(input -> !input.isPresent())
                    .retryIfExceptionOfType(IOException.class)
                    .retryIfRuntimeException()
                    .withRetryListener(new RetryListener() {
                        @Override
                        public void onRetry(Attempt attempt) {
                            if (attempt.hasResult()) {
                                final Object result = attempt.getResult();
                                if (result instanceof Optional && ((Optional<?>) result).isPresent()) {
                                    return;
                                }
                            }
                            probeListener.onRetry(attempt.getAttemptNumber(), connectionAttempts, getAttemptException(attempt));
                        }
                    })
                    .withWaitStrategy(WaitStrategies.fixedWait(delayBetweenAttempts.getQuantity(), delayBetweenAttempts.getUnit()))
                    .withStopStrategy((connectionAttempts == 0) ? StopStrategies.neverStop() : StopStrategies.stopAfterAttempt(connectionAttempts))
                    .build().call(() -> this.probeAllHosts(hosts, probeListener));
        } catch (ExecutionException | RetryException e) {
            probeListener.onError("Unable to retrieve version from indexer node: ", e);
        }
        return Optional.empty();
    }

    @Nullable
    private static Throwable getAttemptException(Attempt attempt) {
        return Optional.of(attempt)
                .filter(Attempt::hasException)
                .map(Attempt::getExceptionCause)
                .orElse(null);
    }

    private Optional<SearchVersion> probeAllHosts(final Collection<URI> hosts, VersionProbeListener listener) {
        return hosts
                .stream()
                .map(host -> probeSingleHost(host, listener))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    private Optional<SearchVersion> probeSingleHost(URI host, VersionProbeListener listener) {
        final Retrofit retrofit;
        try {
            retrofit = new Retrofit.Builder()
                    .baseUrl(host.toURL())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .client(addAuthenticationIfPresent(host, okHttpClient))
                    .build();
        } catch (MalformedURLException e) {
            listener.onError("Indexer node URL is invalid: " + host, e);
            return Optional.empty();
        }

        final RootRoute root = retrofit.create(RootRoute.class);

        final Converter<ResponseBody, ErrorResponse> errorResponseConverter = retrofit.responseBodyConverter(ErrorResponse.class, new Annotation[0]);
        final Consumer<ResponseBody> errorLogger = (responseBody) -> {
            try {
                final ErrorResponse errorResponse = errorResponseConverter.convert(responseBody);
                final String message = String.format(Locale.ROOT, "Unable to retrieve version from indexer node %s:%s: %s", host.getHost(), host.getPort(), errorResponse);
                listener.onError(message, null);
            } catch (IOException e) {
                final String message = String.format(Locale.ROOT, "Unable to retrieve version from indexer node %s:%s: unknown error - an exception occurred while deserializing error response: {}", host.getHost(), host.getPort());
                listener.onError(message, e);
            }
        };


        return rootResponse(root, errorLogger, listener)
                .map(RootResponse::version)
                .flatMap(versionResponse -> parseVersion(versionResponse, listener));
    }

    private Optional<String> getAuthToken(final URI host) {
        if (Strings.emptyToNull(host.getUserInfo()) != null) {
            final String[] credentials = host.getUserInfo().split(":");
            final String username = credentials[0];
            final String password = credentials[1];
            return Optional.of(Credentials.basic(username, password));
        }

        return Optional.empty();
    }

    private OkHttpClient addAuthenticationIfPresent(URI host, OkHttpClient okHttpClient) {
        final Optional<String> authToken = getAuthToken(host);

        if (isJwtAuthentication || authToken.isPresent()) {
            return okHttpClient.newBuilder()
                    .addInterceptor(chain -> {
                        final Request originalRequest = chain.request();
                        final Request.Builder builder = originalRequest.newBuilder().header("Authorization", isJwtAuthentication ? indexerJwtAuthTokenProvider.get() : authToken.get());
                        final Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    })
                    .build();
        }

        return okHttpClient;
    }

    private Optional<SearchVersion> parseVersion(VersionResponse versionResponse, VersionProbeListener probeListener) {
        try {
            final com.github.zafarkhaja.semver.Version version = Version.parse(versionResponse.number());
            return Optional.of(SearchVersion.create(versionResponse.distribution(), version));
        } catch (Exception e) {
            probeListener.onError(String.format(Locale.ROOT, "Unable to parse version retrieved from indexer node: <%s>", versionResponse.number()), e);
            return Optional.empty();
        }
    }

    private Optional<RootResponse> rootResponse(final RootRoute rootRoute, Consumer<ResponseBody> errorLogger, VersionProbeListener listener) {
        try {
            final Response<RootResponse> response = rootRoute.root().execute();
            if (response.isSuccessful()) {
                return Optional.ofNullable(response.body());
            } else {
                errorLogger.accept(response.errorBody());
            }
        } catch (IOException e) {
            final String error = ExceptionUtils.formatMessageCause(e);
            final String rootCause = ExceptionUtils.formatMessageCause(ExceptionUtils.getRootCause(e));
            final String message = String.format(Locale.ROOT, "Unable to retrieve version from indexer node: %s - %s", error, rootCause);
            listener.onError(message, null);
            LOG.debug("Complete exception for version probe error: ", e);
        }
        return Optional.empty();
    }
}
