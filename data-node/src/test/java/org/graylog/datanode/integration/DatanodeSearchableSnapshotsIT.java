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
package org.graylog.datanode.integration;

import com.github.rholder.retry.RetryException;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.graylog.datanode.restoperations.DatanodeOpensearchWait;
import org.graylog.datanode.restoperations.RestOperationParameters;
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.testing.completebackend.S3MinioContainer;
import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;

public class DatanodeSearchableSnapshotsIT {


    private Network network;
    private MongoDBTestService mongoDB;
    private S3MinioContainer s3Container;
    private DatanodeContainerizedBackend datanode;

    @BeforeEach
    void setUp() {
        network = Network.newNetwork();
        mongoDB = MongoDBTestService.create(MongodbServer.DEFAULT_VERSION, network);
        mongoDB.start();
        s3Container = new S3MinioContainer(network);
        s3Container.start();

        datanode = new DatanodeContainerizedBackend(network, mongoDB, "datanode", datanodeContainer -> {
            datanodeContainer.withEnv("GRAYLOG_DATANODE_S3_CLIENT_USER", s3Container.getAccessKey());
            datanodeContainer.withEnv("GRAYLOG_DATANODE_S3_CLIENT_PASSWORD", s3Container.getSecretKey());
            datanodeContainer.withEnv("GRAYLOG_DATANODE_S3_CLIENT_DEFAULT_ENDPOINT", s3Container.getInternalURI().toString());
        });
        datanode.start();
    }

    @AfterEach
    void tearDown() {
        datanode.stop();
        s3Container.stop();
        mongoDB.close();
        network.close();
    }

    @Test
    void testSnapshotCreation() throws ExecutionException, RetryException {

        final var opensearchRestPort = datanode.getOpensearchRestPort();
        final var baseUrl = "http://localhost:" + opensearchRestPort;

        // give opensearch time to start everything
        waitForNode(opensearchRestPort);

        // verify that the s3-repository plugin is installed
        verifyInstalledPlugin(baseUrl, "repository-s3");

        final String bucketName = "my-s3-bucket";
        final String repositoryName = "my-s3-repository";
        createS3Bucket(bucketName);
        createRepository(baseUrl, repositoryName, bucketName);

        createSnapshot(baseUrl, repositoryName)
                .body("snapshot.state", Matchers.equalTo("SUCCESS"));
    }

    private static ValidatableResponse createRepository(String baseUrl, String repositoryName, String bucketName) {
        final String req = """
                {
                  "type": "s3",
                  "settings": {
                    "bucket": "%s",
                    "base_path": "my/snapshot/directory"
                  }
                }
                """.formatted(bucketName);

        return given()
                .body(req)
                .contentType(ContentType.JSON)
                .put(baseUrl + "/_snapshot/" + repositoryName)
                .then()
                .statusCode(200)
                .body("acknowledged", Matchers.equalTo(true));
    }

    private static ValidatableResponse createSnapshot(String baseUrl, String repositoryName) {
        return given().put(baseUrl + "/_snapshot/" + repositoryName + "/1?wait_for_completion=true").then().statusCode(200);
    }

    private void createS3Bucket(String bucketName) {
        try (final S3Client client = s3Container.getClient()) {
            client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
        }
    }

    private static void verifyInstalledPlugin(String baseUrl, String pluginName) {
        given().get(baseUrl + "/_cat/plugins").then().statusCode(200).body(Matchers.containsString(pluginName));
    }

    private void waitForNode(Integer opensearchRestPort) throws ExecutionException, RetryException {
        // this instance is not using any security, no truststore or jwt tokens needed
        new DatanodeOpensearchWait(RestOperationParameters.builder().port(opensearchRestPort).build()).waitForNodesCount(1);
    }
}
