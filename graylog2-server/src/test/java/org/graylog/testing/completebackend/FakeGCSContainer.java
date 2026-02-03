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
package org.graylog.testing.completebackend;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FakeGCSContainer extends GenericContainer<FakeGCSContainer> {
    private static final String IMAGE_NAME = "fsouza/fake-gcs-server";
    private static final int PORT = 4443;
    private final Network network;
    private final String projectId  = "graylog-project";

    public FakeGCSContainer() {
        super(IMAGE_NAME);
        this.network = Network.newNetwork();

        withNetwork(network);
        withNetworkAliases("gcp");
        withExposedPorts(PORT);

        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                "/bin/fake-gcs-server",
                "-scheme", "http"
        ));
    }

    public void updateExternalUrlWithContainerUrl(String fakeGcsExternalUrl) throws Exception {
        String modifyExternalUrlRequestUri = fakeGcsExternalUrl + "/_internal/config";
        String updateExternalUrlJson = "{"
                + "\"externalUrl\": \"" + fakeGcsExternalUrl + "\""
                + "}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(modifyExternalUrlRequestUri))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(updateExternalUrlJson))
                .build();
        HttpResponse<Void> response = HttpClient.newBuilder().build()
                .send(req, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "error updating fake-gcs-server with external url, response status code " + response.statusCode() + " != 200");
        }
    }

    public URI getEndpointUri() {
        return URI.create("http://" + getHost() + ":" + getMappedPort(PORT));
    }

    public Storage getStorage() {
        return StorageOptions.newBuilder()
                .setHost(getEndpointUri().toString())
                .setProjectId(projectId)
                .build()
                .getService();
    }

    public Bucket createBucket(String bucketName) {
        return getStorage().create(
                BucketInfo.newBuilder(bucketName)
                        .build());
    }

    public String getProjectId() {
        return projectId;
    }

    @Override
    public void close() {
        super.close();
        if (network != null) {
            network.close();
        }
    }
}
