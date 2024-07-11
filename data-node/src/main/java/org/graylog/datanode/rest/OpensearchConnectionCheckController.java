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
package org.graylog.datanode.rest;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.graylog.storage.opensearch2.ConnectionCheckRequest;
import org.graylog.storage.opensearch2.ConnectionCheckResponse;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.TrustAllX509TrustManager;
import org.graylog2.security.untrusted.UntrustedCertificateExtractor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Locale;

@Path("/connection-check")
@Produces(MediaType.APPLICATION_JSON)
public class OpensearchConnectionCheckController {

    private final CustomCAX509TrustManager trustManager;

    private final OkHttpClient httpClient;

    @Inject
    public OpensearchConnectionCheckController(CustomCAX509TrustManager trustManager, OkHttpClient httpClient) {
        this.trustManager = trustManager;
        this.httpClient = httpClient;
    }

    @POST
    @Path("opensearch")
    public ConnectionCheckResponse status(ConnectionCheckRequest request) {
        final List<X509Certificate> unknownCertificates = extractUnknownCertificates(request.host());
        try {
            final List<String> indices = getAllIndicesFrom(request.host(), request.username(), request.password(), request.trustUnknownCerts());
            return ConnectionCheckResponse.success(indices, unknownCertificates);
        } catch (Exception e) {
            return ConnectionCheckResponse.error(e, unknownCertificates);
        }
    }

    List<String> getAllIndicesFrom(final String host, final String username, final String password, boolean trustUnknownCerts) {
        var url = (host.endsWith("/") ? host : host + "/") + "_cat/indices?h=index";
        try (var response = getClient(trustUnknownCerts).newCall(new Request.Builder().url(url).header("Authorization", Credentials.basic(username, password)).build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // filtering all indices that start with "." as they indicate a system index - we don't want to reindex those
                return new BufferedReader(new StringReader(response.body().string())).lines().filter(i -> !i.startsWith(".")).sorted().toList();
            } else {
                String message = String.format(Locale.ROOT, "Could not read list of indices from %s. Code=%d, message=%s", host, response.code(), response.message());
                throw new RuntimeException(message);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read list of indices from " + host + ", " + e.getMessage(), e);
        }
    }


    private OkHttpClient getClient(boolean trustUnknownCerts) {
        if (trustUnknownCerts) {
            try {
                final SSLContext ctx = SSLContext.getInstance("TLS");
                final TrustAllX509TrustManager trustManager = new TrustAllX509TrustManager();
                ctx.init(null, new TrustManager[]{trustManager}, new SecureRandom());
                return httpClient.newBuilder().sslSocketFactory(ctx.getSocketFactory(), trustManager).build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        } else {
            return httpClient;
        }
    }

    @Nonnull
    private List<X509Certificate> extractUnknownCertificates(String host)  {
        final UntrustedCertificateExtractor extractor = new UntrustedCertificateExtractor(httpClient);
        try {
            return extractor.extractUntrustedCerts(host);
        } catch (NoSuchAlgorithmException | IOException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
}
