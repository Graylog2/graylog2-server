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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.graylog2.security.DefaultX509TrustManager;
import org.graylog2.security.TrustAllX509TrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.ExecutionException;

@Singleton
public class ParameterizedHttpClientProvider {
    private static final Logger LOG = LoggerFactory.getLogger(ParameterizedHttpClientProvider.class);

    private final LoadingCache<Parameters, OkHttpClient> cache;
    private final OkHttpClientProvider okHttpClientProvider;

    @Inject
    public ParameterizedHttpClientProvider(OkHttpClientProvider provider) {
        okHttpClientProvider = provider;
        cache = CacheBuilder.newBuilder().build(CacheLoader.from(this::buildHttpClient));
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
            case KEEPALIVE -> withKeepAlive(builder);
            case SKIP_TLS_VERIFY -> withSkipTLSVerification(builder);
            case SKIP_TLS_VERIFY_AND_KEEPALIVE -> withSkipTLSVerificationAndKeepAlive(builder);
        }
        return builder.build();
    }

    private Builder withKeepAlive(Builder builder) {
        builder.socketFactory(new TcpKeepAliveSocketFactory(SocketFactory.getDefault()));
        try {
            builder.sslSocketFactory(new TcpKeepAliveSSLSocketFactory(SSLContext.getDefault().getSocketFactory()), new DefaultX509TrustManager("ignored"));
        } catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOG.error("Failed to apply SSL TCP keep-alive to OkHttpClient", e);
        }
        return builder;
    }

    private Builder withSkipTLSVerification(Builder builder) {
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final X509TrustManager trustManager = new TrustAllX509TrustManager();
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            builder.hostnameVerifier((h, s) -> true);
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOG.error("Failed to skip TLS verification on OkHttpClient", e);
        }
        return builder;
    }

    private Builder withSkipTLSVerificationAndKeepAlive(Builder builder) {
        builder.socketFactory(new TcpKeepAliveSocketFactory(SocketFactory.getDefault()));
        try {
            final SSLContext sslContext = SSLContext.getInstance("TLS");
            final X509TrustManager trustManager = new TrustAllX509TrustManager();
            sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            builder.sslSocketFactory(new TcpKeepAliveSSLSocketFactory(sslContext.getSocketFactory()), trustManager);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            LOG.error("Failed to skip TLS verification / enable TCP keep-alive on OkHttpClient", e);
        }
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

