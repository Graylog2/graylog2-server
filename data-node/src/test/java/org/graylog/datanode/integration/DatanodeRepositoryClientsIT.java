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
import org.graylog.datanode.testinfra.DatanodeContainerizedBackend;
import org.graylog.testing.completebackend.ContainerizedGraylogBackend;
import org.graylog.testing.completebackend.S3MinioContainer;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.restoperations.DatanodeOpensearchWait;
import org.graylog.testing.restoperations.RestOperationParameters;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.datanode.repositories.RepositoryClientService;
import org.graylog2.datanode.repositories.RepositoryClientSettings;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static io.restassured.RestAssured.given;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Stores repository clients like the Graylog server does and verifies that the Data Node gives each repository its
 * own working opensearch S3 client.
 */
public class DatanodeRepositoryClientsIT {

    private static final String BUCKET = "warm-tier";

    private Network network;
    private MongoDBTestService mongoDB;
    private S3MinioContainer s3Container;
    private DatanodeContainerizedBackend datanode;

    @BeforeEach
    void setUp() {
        network = Network.newNetwork();
        mongoDB = MongoDBTestService.createStarted(network);
        s3Container = new S3MinioContainer(network);
        s3Container.start();
        try (final S3Client client = s3Container.getClient()) {
            client.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
        }
    }

    @AfterEach
    void tearDown() {
        if (datanode != null) {
            datanode.stop();
        }
        s3Container.stop();
        mongoDB.close();
        network.close();
    }

    @Test
    void repositoriesGetTheirOwnClients() throws ExecutionException, RetryException {
        // Credentials are read when the opensearch configuration is built, so they have to exist before the start
        final String valid = saveCredentials("valid", s3Container.getSecretKey());
        final String invalid = saveCredentials("invalid", "wrong-secret");
        final String baseUrl = startDatanode();

        registerRepository(baseUrl, "valid", valid)
                .statusCode(200)
                .body("acknowledged", Matchers.equalTo(true));
        given().put(f("%s/_snapshot/valid/1?wait_for_completion=true", baseUrl))
                .then()
                .statusCode(200)
                .body("snapshot.state", Matchers.equalTo("SUCCESS"));

        // The other repository uses its own client with its own (wrong) credentials, so verification fails
        registerRepository(baseUrl, "invalid", invalid)
                .statusCode(500)
                .body("error.type", Matchers.equalTo("repository_verification_exception"));
    }

    private String saveCredentials(String repositoryName, String secretKey) {
        final MongoCollections mongoCollections = new MongoCollections(
                new MongoJackObjectMapperProvider(new ObjectMapperProvider().get()), mongoDB.mongoConnection());
        // The Data Node decrypts the secrets with its password_secret, which has to match the server's
        final EncryptedValueService encryptedValueService = new EncryptedValueService(ContainerizedGraylogBackend.PASSWORD_SECRET);
        final URI endpoint = s3Container.getInternalURI();
        final RepositoryClientSettings settings = new RepositoryClientSettings(RepositoryClientSettings.TYPE_S3,
                Map.of("protocol", endpoint.getScheme(),
                        "endpoint", f("%s:%d", endpoint.getHost(), endpoint.getPort()),
                        "region", "us-east-1",
                        "path_style_access", "true"),
                Map.of("access_key", encryptedValueService.encrypt(s3Container.getAccessKey()),
                        "secret_key", encryptedValueService.encrypt(secretKey)),
                Map.of());
        return new RepositoryClientService(mongoCollections).create(repositoryName, client -> settings).clientName();
    }

    private String startDatanode() throws ExecutionException, RetryException {
        datanode = new DatanodeContainerizedBackend(network, mongoDB, "datanode", container -> {});
        try {
            datanode.start();
        } catch (Exception e) {
            throw new IllegalStateException(f("Data Node failed to start:%n%s", datanode.getLogs()), e);
        }
        final int port = datanode.getOpensearchRestPort();
        // this instance is not using any security, no truststore or jwt tokens needed
        new DatanodeOpensearchWait(RestOperationParameters.builder().port(port).build()).waitForGreenStatusAndNodesCount(1);
        return f("http://localhost:%d", port);
    }

    private static ValidatableResponse registerRepository(String baseUrl, String repositoryName, String clientName) {
        final String request = f("""
                {
                  "type": "s3",
                  "settings": {
                    "bucket": "%s",
                    "base_path": "%s",
                    "client": "%s"
                  }
                }
                """, BUCKET, repositoryName, clientName);
        return given()
                .body(request)
                .contentType(ContentType.JSON)
                .put(f("%s/_snapshot/%s", baseUrl, repositoryName))
                .then();
    }
}
