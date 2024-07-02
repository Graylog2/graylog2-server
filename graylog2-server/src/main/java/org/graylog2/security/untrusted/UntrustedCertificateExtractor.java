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
package org.graylog2.security.untrusted;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This extractor will configure a {@link CertificateChainReportingTrustManager} to augment the certificate exception
 * that we expect during the get request to the host. When the SSLHandshakeException happens, we'll use the exception
 * to access the original certificate chain that has been provided by the host. The chain is then returned for further
 * processing.
 */
public class UntrustedCertificateExtractor {
    private final OkHttpClient httpClient;

    public UntrustedCertificateExtractor(OkHttpClient okHttpClient) {
        this.httpClient = okHttpClient;
    }

    public List<X509Certificate> extractUntrustedCerts(String host) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        final SSLContext ctx = SSLContext.getInstance("TLS");
        // this trust manager is augmenting the SSLHandshakeException cause, so we can later in the catch block
        // access the certificate chain.
        final X509TrustManager trustManager = new CertificateChainReportingTrustManager(httpClient.x509TrustManager());
        ctx.init(null, new TrustManager[]{trustManager}, new SecureRandom());

        final OkHttpClient reportingClient = httpClient.newBuilder().sslSocketFactory(ctx.getSocketFactory(), trustManager).build();

        final Request req = new Request.Builder().get().url(host).build();
        try(final Response ignored = reportingClient.newCall(req).execute()) {
            // nothing to do here, we are interested only in exceptions
        } catch (SSLHandshakeException e) {
            if (e.getCause() instanceof CertificateReportingException certificateReportingException) {
                return Arrays.asList(certificateReportingException.getChain());
            }
        }
        return Collections.emptyList();
    }
}
