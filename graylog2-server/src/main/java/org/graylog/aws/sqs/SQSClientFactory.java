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
package org.graylog.aws.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import org.graylog.aws.notifications.SQSClient;
import org.graylog2.plugin.InputFailureRecorder;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

public class SQSClientFactory {
    private final ObjectMapper objectMapper;

    @Inject
    public SQSClientFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SQSClient create(String queueName, String region, AwsCredentialsProvider credentialsProvider, InputFailureRecorder inputFailureRecorder) {
        return new SQSClient(queueName, region, credentialsProvider, objectMapper, inputFailureRecorder);
    }
}
