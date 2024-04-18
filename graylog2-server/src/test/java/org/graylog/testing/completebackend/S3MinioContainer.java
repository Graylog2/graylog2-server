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

import org.apache.commons.lang.RandomStringUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public class S3MinioContainer extends GenericContainer<S3MinioContainer> {
    private static final String IMAGE_NAME = "minio/minio:latest";
    private static final int PORT = 9000;
    private final String accessKey;
    private final String secretKey;
    // if we create a network in this instance with 'Network.newNetwork()', we have to close it, too
    private final boolean closeNetwork;
    private Optional<Network> network = Optional.empty();

    public S3MinioContainer() {
        this(Network.newNetwork(), true);
    }

    public S3MinioContainer(Network network) {
        this(network, false);
    }

    public S3MinioContainer(Network network, final boolean closeNetwork) {
        super(IMAGE_NAME);
        this.closeNetwork = closeNetwork;
        this.network = Optional.of(network);

        accessKey = RandomStringUtils.randomAlphanumeric(10);
        secretKey = RandomStringUtils.randomAlphanumeric(10);

        withCommand("server /data");
        withNetwork(network);
        withNetworkAliases("minio");
        withExposedPorts(PORT);
        // Try to support virtual-host-style requests.
        withEnv("MINIO_DOMAIN", "localhost," + String.join(",", getNetworkAliases()));
        withEnv("MINIO_ACCESS_KEY", accessKey);
        withEnv("MINIO_SECRET_KEY", secretKey);
        withEnv("MINIO_BROWSER", "off");

        waitingFor(new HttpWaitStrategy().forPath("/minio/health/ready").forPort(PORT).withStartupTimeout(Duration.ofSeconds(10)));
    }

    public URI getEndpointURI() {
        return URI.create("http://" + getHost() + ":" + getMappedPort(PORT));
    }

    public URI getInternalURI() {
        return URI.create("http://" + getNetworkAliases().get(0) + ":" + PORT);
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public S3Client getClient() {
        return S3Client.builder()
                .region(Region.of("eu-west-1"))
                .endpointOverride(getEndpointURI())
                .httpClientBuilder(ApacheHttpClient.builder())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(getAccessKey(), getSecretKey())))
                .forcePathStyle(true)
                .build();
    }

    @Override
    public void close() {
        super.close();
        if (closeNetwork) {
            network.ifPresent(Network::close);
        }
    }
}
