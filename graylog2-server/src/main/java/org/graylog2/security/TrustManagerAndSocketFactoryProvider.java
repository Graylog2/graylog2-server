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
package org.graylog2.security;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Singleton
public class TrustManagerAndSocketFactoryProvider {
    private final SSLSocketFactory sslSocketFactory;
    private final X509TrustManager trustManager;
    private final SSLContext sslContext;

    @Inject
    public TrustManagerAndSocketFactoryProvider(X509TrustManager trustManager) throws NoSuchAlgorithmException, KeyManagementException {
        this.sslContext = SSLContext.getInstance("TLS");
        this.trustManager = trustManager;
        sslContext.init(null, new TrustManager[]{this.trustManager}, new SecureRandom());
        this.sslSocketFactory = sslContext.getSocketFactory();
    }

    public SSLSocketFactory getSslSocketFactory() {
        return this.sslSocketFactory;
    }

    public X509TrustManager getTrustManager() {
        return trustManager;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }
}
