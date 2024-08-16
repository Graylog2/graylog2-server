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

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.graylog.datanode.configuration.DatanodeTrustManagerProvider;
import org.graylog.storage.opensearch2.IndexState;
import org.graylog.storage.opensearch2.IndexStateChangeRequest;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Objects;

import static org.graylog.datanode.rest.OpensearchConnectionCheckController.CONNECT_TIMEOUT;
import static org.graylog.datanode.rest.OpensearchConnectionCheckController.READ_TIMEOUT;
import static org.graylog.datanode.rest.OpensearchConnectionCheckController.WRITE_TIMEOUT;

@Path("/index-state")
@Produces(MediaType.APPLICATION_JSON)
public class IndexStateController {

    private final DatanodeTrustManagerProvider datanodeTrustManagerProvider;
    private final OkHttpClient httpClient;

    @Inject
    public IndexStateController(DatanodeTrustManagerProvider datanodeTrustManagerProvider) {
        this.datanodeTrustManagerProvider = datanodeTrustManagerProvider;
        this.httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(CONNECT_TIMEOUT)
                .writeTimeout(WRITE_TIMEOUT)
                .readTimeout(READ_TIMEOUT)
                .build();
    }

    @POST
    @Path("/get")
    public IndexState get(IndexStateChangeRequest indexStateChangeRequest) {
        final String host = indexStateChangeRequest.host().endsWith("/") ? indexStateChangeRequest.host() : indexStateChangeRequest.host() + "/";
        final Request.Builder request = new Request.Builder()
                .url(host + "_cat/indices/" + indexStateChangeRequest.indexName() + "/?h=status");
        if (Objects.nonNull(indexStateChangeRequest.username()) && Objects.nonNull(indexStateChangeRequest.password())) {
            request.header("Authorization", Credentials.basic(indexStateChangeRequest.username(), indexStateChangeRequest.password()));
        }
        try (var response = getClient().newCall(request.build()).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                final String state = response.body().string().trim().toUpperCase(Locale.ROOT);
                return IndexState.valueOf(state);
            } else {
                throw new RuntimeException("Failed to detect open/close index status " + indexStateChangeRequest.indexName() + ". Code: " + response.code() + "; message=" + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open/close index" + indexStateChangeRequest.indexName(), e);
        }
    }

    @POST
    @Path("/set")
    public IndexState change(IndexStateChangeRequest indexStateChangeRequest) {
        return performAction(indexStateChangeRequest);
    }

    private IndexState performAction(IndexStateChangeRequest indexStateChangeRequest) {
        final String host = indexStateChangeRequest.host().endsWith("/") ? indexStateChangeRequest.host() : indexStateChangeRequest.host() + "/";
        final Request.Builder request = new Request.Builder()
                .post(RequestBody.create("", okhttp3.MediaType.parse(MediaType.APPLICATION_JSON)))
                .url(host + indexStateChangeRequest.indexName() + "/" + (indexStateChangeRequest.action() == IndexState.OPEN ? "_open" : "_close"));
        if (Objects.nonNull(indexStateChangeRequest.username()) && Objects.nonNull(indexStateChangeRequest.password())) {
            request.header("Authorization", Credentials.basic(indexStateChangeRequest.username(), indexStateChangeRequest.password()));
        }
        try (var response = getClient().newCall(request.build()).execute()) {
            if (response.isSuccessful()) {
                return indexStateChangeRequest.action();
            } else {
                throw new RuntimeException("Failed to open/close index " + indexStateChangeRequest.indexName() + ". Code: " + response.code() + "; message=" + response.message());
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to open/close index" + indexStateChangeRequest.indexName(), e);
        }
    }

    private OkHttpClient getClient() {
        try {
            final SSLContext ctx = SSLContext.getInstance("TLS");
            final X509TrustManager trustManager = datanodeTrustManagerProvider.get();
            ctx.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return httpClient.newBuilder().sslSocketFactory(ctx.getSocketFactory(), trustManager).build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);

        }
    }
}
