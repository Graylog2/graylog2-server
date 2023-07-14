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
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Converter;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;

public class VersionProbe {
    private static final Logger LOG = LoggerFactory.getLogger(VersionProbe.class);
    private final ObjectMapper objectMapper;
    private final OkHttpClient okHttpClient;
    private final int connectionAttempts;
    private final Duration delayBetweenAttempts;
    private final TrustManagerProvider trustManagerProvider;

    @Inject
    public VersionProbe(ObjectMapper objectMapper,
                        OkHttpClient okHttpClient,
                        @Named("elasticsearch_version_probe_attempts") int elasticsearchVersionProbeAttempts,
                        @Named("elasticsearch_version_probe_delay") Duration elasticsearchVersionProbeDelay,
                        TrustManagerProvider trustManagerProvider) {
        this.objectMapper = objectMapper;
        this.okHttpClient = okHttpClient;
        this.connectionAttempts = elasticsearchVersionProbeAttempts;
        this.delayBetweenAttempts = elasticsearchVersionProbeDelay;
        this.trustManagerProvider = trustManagerProvider;
    }

    public Optional<SearchVersion> probe(final Collection<URI> hosts) {
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
                            if (connectionAttempts == 0) {
                                LOG.info("Elasticsearch is not available. Retry #{}", attempt.getAttemptNumber());
                            } else {
                                LOG.info("Elasticsearch is not available. Retry #{}/{}", attempt.getAttemptNumber(), connectionAttempts);
                            }
                        }
                    })
                    .withWaitStrategy(WaitStrategies.fixedWait(delayBetweenAttempts.getQuantity(), delayBetweenAttempts.getUnit()))
                    .withStopStrategy((connectionAttempts == 0) ? StopStrategies.neverStop() : StopStrategies.stopAfterAttempt(connectionAttempts))
                    .build().call(() -> this.probeAllHosts(hosts));
        } catch (ExecutionException | RetryException e) {
            LOG.error("Unable to retrieve version from Elasticsearch node: ", e);
        }
        return Optional.empty();
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
                    .client(clientWithAdditions(host, okHttpClient))
                    .build();
        } catch (MalformedURLException e) {
            LOG.error("Elasticsearch node URL is invalid: " + host.toString(), e);
            return Optional.empty();
        }

        final RootRoute root = retrofit.create(RootRoute.class);

        final Converter<ResponseBody, ErrorResponse> errorResponseConverter = retrofit.responseBodyConverter(ErrorResponse.class, new Annotation[0]);
        final Consumer<ResponseBody> errorLogger = (responseBody) -> {
            try {
                final ErrorResponse errorResponse = errorResponseConverter.convert(responseBody);
                LOG.error("Unable to retrieve version from Elasticsearch node {}:{}: {}", host.getHost(), host.getPort(), errorResponse);
            } catch (IOException e) {
                LOG.error("Unable to retrieve version from Elasticsearch node {}:{}: unknown error - an exception occurred while deserializing error response: {}", host.getHost(), host.getPort(), e);
            }
        };


        return rootResponse(root, errorLogger)
                .map(RootResponse::version)
                .flatMap(this::parseVersion);
    }

    private OkHttpClient clientWithAdditions(final URI host, final OkHttpClient okHttpClient) {
        var builder = okHttpClient.newBuilder();
        addSSLContextIfHttps(host, builder);
        addAuthenticationIfPresent(host, builder);
        return builder.build();
    }

    private void addSSLContextIfHttps(final URI host, final OkHttpClient.Builder okHttpClient) {
        if("https".equalsIgnoreCase(host.getScheme())) {
            try {
                final var sslContext = SSLContext.getInstance("TLS");
                final var tm = trustManagerProvider.create(host.getHost());
                sslContext.init(null, new TrustManager[]{tm}, new SecureRandom());
                okHttpClient.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager)tm);

                if (!isNullOrEmpty(host.getUserInfo())) {
                    var list = Splitter.on(":").limit(2).splitToList(host.getUserInfo());
                    okHttpClient.authenticator((route, response) -> {
                        String credential = Credentials.basic(list.get(0), list.get(1));
                        return response.request().newBuilder().header("Authorization", credential).build();
                    });
                }
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
                LOG.error("Could not set Graylog CA trustmanager: {}", ex.getMessage(), ex);
            }
        }
    }

    private void addAuthenticationIfPresent(final URI host, final OkHttpClient.Builder okHttpClient) {
        if (Strings.emptyToNull(host.getUserInfo()) != null) {
            final String[] credentials = host.getUserInfo().split(":");
            final String username = credentials[0];
            final String password = credentials[1];
            final String authToken = Credentials.basic(username, password);

            okHttpClient
                    .addInterceptor(chain -> {
                        final Request originalRequest = chain.request();
                        final Request.Builder builder = originalRequest.newBuilder().header("Authorization", authToken);
                        final Request newRequest = builder.build();
                        return chain.proceed(newRequest);
                    });
        }
    }

    private Optional<SearchVersion> parseVersion(VersionResponse versionResponse) {
        try {
            final com.github.zafarkhaja.semver.Version version = com.github.zafarkhaja.semver.Version.valueOf(versionResponse.number());
            return Optional.of(SearchVersion.create(versionResponse.distribution(), version));
        } catch (Exception e) {
            LOG.error("Unable to parse version retrieved from Elasticsearch node: <{}>", versionResponse.number(), e);
            return Optional.empty();
        }
    }

    private Optional<RootResponse> rootResponse(final RootRoute rootRoute, Consumer<ResponseBody> errorLogger) {
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
            LOG.error("Unable to retrieve version from Elasticsearch node: {} - {}", error, rootCause);
            LOG.info("Complete exception for version probe error: ", e);
        }
        return Optional.empty();
    }
}
