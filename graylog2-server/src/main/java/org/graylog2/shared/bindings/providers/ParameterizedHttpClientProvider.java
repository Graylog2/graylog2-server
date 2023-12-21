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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.graylog2.security.DefaultX509TrustManager;
import org.graylog2.security.TrustAllX509TrustManager;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

@Singleton
public class ParameterizedHttpClientProvider {
    private final LoadingCache<Parameters, OkHttpClient> cache;
    private final OkHttpClientProvider okHttpClientProvider;
    private final X509TrustManager insecureTrustManager;
    private final SSLSocketFactory insecureSocketFactory;
    private X509TrustManager trustManager;
    private SSLSocketFactory sslSocketFactory;

    @Inject
    public ParameterizedHttpClientProvider(OkHttpClientProvider provider) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException {
        okHttpClientProvider = provider;
        cache = CacheBuilder.newBuilder().build(CacheLoader.from(this::buildHttpClient));

        insecureTrustManager = new TrustAllX509TrustManager();
        SSLContext insecureSSLContext = SSLContext.getInstance("TLS");
        insecureSSLContext.init(null, new TrustManager[]{insecureTrustManager}, new SecureRandom());
        insecureSocketFactory = insecureSSLContext.getSocketFactory();

        sslSocketFactory = SSLContext.getDefault().getSocketFactory();
        trustManager = new DefaultX509TrustManager("ignored");
    }

    @VisibleForTesting
    // allows testing with a custom trustManager / sslSocketFactory
    public ParameterizedHttpClientProvider(OkHttpClientProvider provider, SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        this(provider);
        this.trustManager = trustManager;
        this.sslSocketFactory = sslSocketFactory;
    }

    public OkHttpClient get(boolean keepAlive, boolean skipTLSVerify) {
        try {
            return cache.get(Parameters.fromBoolean(keepAlive, skipTLSVerify));
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private OkHttpClient buildHttpClient(Parameters parameters) {
        final Builder builder = okHttpClientProvider.get().newBuilder();

        switch (parameters) {
            case NONE -> withDefaultSocketFactory(builder);
            case KEEPALIVE -> withKeepAlive(builder);
            case SKIP_TLS_VERIFY -> withSkipTLSVerification(builder);
            case SKIP_TLS_VERIFY_AND_KEEPALIVE -> withSkipTLSVerificationAndKeepAlive(builder);
        }
        return builder.build();
    }

    private Builder withDefaultSocketFactory(Builder builder) {
        builder.sslSocketFactory(sslSocketFactory, trustManager);
        return builder;
    }

    private Builder withKeepAlive(Builder builder) {
        builder.socketFactory(new TcpKeepAliveSocketFactory(SocketFactory.getDefault()));
        builder.sslSocketFactory(new TcpKeepAliveSSLSocketFactory(sslSocketFactory), trustManager);
        return builder;
    }

    private Builder withSkipTLSVerification(Builder builder) {
        builder.sslSocketFactory(insecureSocketFactory, insecureTrustManager);
        builder.hostnameVerifier((h, s) -> true);
        return builder;
    }

    private Builder withSkipTLSVerificationAndKeepAlive(Builder builder) {
        builder.socketFactory(new TcpKeepAliveSocketFactory(SocketFactory.getDefault()));
        builder.sslSocketFactory(new TcpKeepAliveSSLSocketFactory(insecureSocketFactory), insecureTrustManager);
        builder.hostnameVerifier((h, s) -> true);
        return builder;
    }

    enum Parameters {
        NONE,
        KEEPALIVE,
        SKIP_TLS_VERIFY,
        SKIP_TLS_VERIFY_AND_KEEPALIVE;

        static Parameters fromBoolean(boolean keepAlive, boolean skipTLSVerify) {
            if (keepAlive) {
                if (skipTLSVerify) {
                    return SKIP_TLS_VERIFY_AND_KEEPALIVE;
                }
                return KEEPALIVE;
            }
            if (skipTLSVerify) {
                return SKIP_TLS_VERIFY;
            }
            return NONE;
        }
    }
}

