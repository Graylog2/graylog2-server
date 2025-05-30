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
package org.graylog2.inputs;

import org.graylog.testing.completebackend.Lifecycle;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTest;
import org.graylog.testing.containermatrix.annotations.ContainerMatrixTestsConfiguration;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;

@ContainerMatrixTestsConfiguration(serverLifecycle = Lifecycle.CLASS)
public class InputCreationIT {

    private final GraylogApis apis;

    public InputCreationIT(GraylogApis apis) {
        this.apis = apis;
    }

    @ContainerMatrixTest
    void testHttpRandomInputCreation() {
        String inputId = apis.inputs().createGlobalInput("testInput",
                "org.graylog2.inputs.random.FakeHttpMessageInput",
                Map.of("sleep", 30,
                        "sleep_deviation", 30,
                        "source", "example.org"));
        apis.inputs().getInput(inputId)
                .assertThat().body("title", equalTo("testInput"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId)
                                .extract().body().jsonPath().get("state")
                                .equals("RUNNING"),
                "Timed out waiting for HTTP Random Message Input to become available");
        apis.inputs().deleteInput(inputId);
    }

    /**
     * Test to make sure configuration encryption serialization/deserialization works
     */
    @ContainerMatrixTest
    void testFailingAwsCloudTrailInputCreation() {
        String inputId = apis.inputs().createGlobalInput("testInput",
                "org.graylog.aws.inputs.cloudtrail.CloudTrailInput",
                Map.of("aws_sqs_region", "us-east-1",
                        "aws_s3_region", "us-east-1",
                        "aws_sqs_queue_name", "invalid-queue-no-messages-read",
                        "aws_access_key", "invalid-access-key",
                        "aws_secret_key", "invalid-secret-key"));
        apis.inputs().getInput(inputId)
                .assertThat().body("attributes.aws_access_key", equalTo("invalid-access-key"));
        apis.waitFor(() ->
                        apis.inputs().getInputState(inputId)
                                .extract().body().jsonPath().get("state")
                                .equals("FAILING"),
                "Timed out waiting for AWS CloudTrail Input to reach failing state");
        apis.inputs().deleteInput(inputId);
    }
}
