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
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import jakarta.annotation.Nullable;
import jakarta.inject.Named;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.graylog2.configuration.RunsWithDataNode;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.security.jwt.IndexerJwtAuthTokenProvider;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.storage.SearchVersion;
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

public class VersionProbeImpl implements VersionProbe {

    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final IndexerJwtAuthToken jwtAuthToken;
    private final VersionProbeListener probeListener;
    private final Duration delayBetweenAttempts;
    private final int connectionAttempts;

    @AssistedInject
    VersionProbeImpl(ObjectMapper objectMapper,
                     OkHttpClient okHttpClient,
                     IndexerJwtAuthToken indexerJwtAuthToken,
                     @Named("elasticsearch_version_probe_attempts") int elasticsearchVersionProbeAttempts,
                     @Named("elasticsearch_version_probe_delay") Duration elasticsearchVersionProbeDelay) {
        this(
                objectMapper,
                okHttpClient,
                indexerJwtAuthToken,
                elasticsearchVersionProbeAttempts,
                elasticsearchVersionProbeDelay,
                VersionProbeLogger.INSTANCE);
    }

    @AssistedInject
    VersionProbeImpl(ObjectMapper objectMapper,
                     OkHttpClient okHttpClient,
                     @Assisted IndexerJwtAuthToken indexerJwtAuthToken,
                     @Assisted int elasticsearchVersionProbeAttempts,
                     @Assisted Duration elasticsearchVersionProbeDelay,
                     @Assisted VersionProbeListener probeListener) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.connectionAttempts = elasticsearchVersionProbeAttempts;
        this.probeListener = probeListener;
        this.delayBetweenAttempts = elasticsearchVersionProbeDelay;
        this.jwtAuthToken = indexerJwtAuthToken;
    }

    public Optional<SearchVersion> probe(final Collection<URI> hosts) {
        try {
            return RetryerBuilder.<Optional<SearchVersion>>newBuilder()
                    .retryIfResult(input -> input.isEmpty())
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
                    .build().call(() -> this.probeAllHosts(hosts));
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

    private Optional<SearchVersion> probeAllHosts(final Collection<URI> hosts) {
        return hosts
                .stream()
                .map(this::probeSingleHost)
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    private Optional<SearchVersion> probeSingleHost(URI host) {
        final Retrofit retrofit;
        try {
            retrofit = new Retrofit.Builder()
                    .baseUrl(host.toURL())
                    .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                    .client(addAuthenticationIfPresent(host, okHttpClient))
                    .build();
        } catch (MalformedURLException e) {
            probeListener.onError("Indexer node URL is invalid: " + host, e);
            return Optional.empty();
        }

        final RootRoute root = retrofit.create(RootRoute.class);

        final Converter<ResponseBody, ErrorResponse> errorResponseConverter = retrofit.responseBodyConverter(ErrorResponse.class, new Annotation[0]);
        final Consumer<ResponseBody> errorLogger = (responseBody) -> {
            try {
                final ErrorResponse errorResponse = errorResponseConverter.convert(responseBody);
                final String message = String.format(Locale.ROOT, "Unable to retrieve version from indexer node %s:%s: %s", host.getHost(), host.getPort(), errorResponse);
                probeListener.onError(message, null);
            } catch (IOException e) {
                final String message = String.format(Locale.ROOT, "Unable to retrieve version from indexer node %s:%s: unknown error - an exception occurred while deserializing error response: {}", host.getHost(), host.getPort());
                probeListener.onError(message, e);
            }
        };


        return rootResponse(root, errorLogger, probeListener)
                .map(RootResponse::version)
                .flatMap(versionResponse -> parseVersion(versionResponse, probeListener));
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
        return jwtAuthToken.headerValue().or(() -> getAuthToken(host))
                .map(authToken -> okHttpClient.newBuilder()
                        .addInterceptor(chain -> {
                            final Request originalRequest = chain.request();
                            final Request.Builder builder = originalRequest.newBuilder().header("Authorization", authToken);
                            final Request newRequest = builder.build();
                            return chain.proceed(newRequest);
                        })
                        .build())
                .orElse(okHttpClient);
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
