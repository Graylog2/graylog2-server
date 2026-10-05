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
package org.graylog.integrations.aws.transports;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.net.URI;
import java.time.Duration;

/**
 * Minimal AWS emulator container providing Kinesis and DynamoDB for KCL integration tests.
 * Runs Floci (MIT-licensed, LocalStack-API-compatible). LocalStack itself is not an option:
 * its free tier prohibits commercial use since March 2026 and the image requires an auth token.
 * Uses a plain GenericContainer (like {@code S3MinioContainer}) to avoid extra test dependencies.
 * Since {@code SERVICES} does not include {@code cloudwatch}, KCL consumers under test must disable
 * metrics ({@code MetricsLevel.NONE}), which the seam-based tests do.
 */
class KinesisEmulatorContainer extends GenericContainer<KinesisEmulatorContainer> {
    private static final String IMAGE = "floci/floci:1.5.33";
    private static final int PORT = 4566;

    KinesisEmulatorContainer() {
        super(IMAGE);
        withExposedPorts(PORT);
        withEnv("SERVICES", "kinesis,dynamodb");
        // Forward container output to the test log so CI failures are diagnosable.
        withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(KinesisEmulatorContainer.class)));
        waitingFor(new HttpWaitStrategy()
                .forPath("/_localstack/health")
                .forPort(PORT)
                .withStartupTimeout(Duration.ofSeconds(60)));
    }

    URI endpoint() {
        return URI.create("http://" + getHost() + ":" + getMappedPort(PORT));
    }
}
