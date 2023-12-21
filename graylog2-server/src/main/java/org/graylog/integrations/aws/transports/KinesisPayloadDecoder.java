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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.integrations.aws.AWSMessageType;
import org.graylog.integrations.aws.cloudwatch.CloudWatchLogSubscriptionData;
import org.graylog.integrations.aws.cloudwatch.KinesisLogEntry;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for decoding the raw Kinesis byte array payload.
 */
public class KinesisPayloadDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(KinesisPayloadDecoder.class);

    private final ObjectMapper objectMapper;
    private final AWSMessageType awsMessageType;
    private final String kinesisStream;

    @Inject
    public KinesisPayloadDecoder(ObjectMapper objectMapper, AWSMessageType awsMessageType, String kinesisStream) {
        this.objectMapper = objectMapper;
        this.awsMessageType = awsMessageType;
        this.kinesisStream = kinesisStream;
    }

    /**
     * Decodes the raw Kinesis byte array message payload.
     *
     * <p>The following {@link AWSMessageType} enum values are supported:</p>
     *
     * <p>
     * <strong>{@code AWSMessageType.KINESIS_RAW}</strong>: Raw Kinesis log messages that are converted directly to a string.
     * <strong>{@code AWSMessageType.KINESIS_FLOW_LOGS}</strong>: CloudWatch Flowlog messages. These messages are
     * delivered to Kinesis from CloudWatch in batches within a JSON document via
     * <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">CloudWatch Subscription Filters</a>.
     * </p>
     *
     * @param payloadBytes                A Kinesis payload in byte array form.
     * @param approximateArrivalTimestamp The approximate instant that the message was written to Kinesis. This is used only
     *                                    for the {@code AWSMessageType.KINESIS_RAW} message timestamp.
     * @return A list of {@link KinesisLogEntry} messages, which are fully ready to be written to the Graylog Journal.
     * @throws IOException
     */
    List<KinesisLogEntry> processMessages(final byte[] payloadBytes, Instant approximateArrivalTimestamp) throws IOException {

        // This method will be called from a codec, and therefore will not perform any detection. It will rely
        // exclusively on the AWSMessageType detected in the setup HealthCheck.
        // If a user needs to change the type of data stored in a stream, they will need to set the integration up again.
        if (awsMessageType == AWSMessageType.KINESIS_CLOUDWATCH_FLOW_LOGS || awsMessageType == AWSMessageType.KINESIS_CLOUDWATCH_RAW) {
            final CloudWatchLogSubscriptionData logSubscriptionData = decompressCloudWatchMessages(payloadBytes, objectMapper);
            return logSubscriptionData.logEvents().stream()
                    .map(le -> {
                        DateTime timestamp = new DateTime(le.timestamp(), DateTimeZone.UTC);
                        return KinesisLogEntry.create(kinesisStream,
                                // Use the log group and stream returned from CloudWatch.
                                logSubscriptionData.logGroup(),
                                logSubscriptionData.logStream(), timestamp, le.message());
                    })
                    .collect(Collectors.toList());
        } else if (awsMessageType == AWSMessageType.KINESIS_RAW) {
            // The best timestamp available is the approximate arrival time of the message to the Kinesis stream.
            final DateTime timestamp = new DateTime(approximateArrivalTimestamp.toEpochMilli(), DateTimeZone.UTC);
            final KinesisLogEntry kinesisLogEntry = KinesisLogEntry.create(kinesisStream,
                    "", "",
                    timestamp, new String(payloadBytes, StandardCharsets.UTF_8));
            return Collections.singletonList(kinesisLogEntry);
        } else {
            LOG.error("The AWSMessageType [{}] is not supported by the KinesisTransport", awsMessageType);
            return new ArrayList<>();
        }
    }

    /**
     * Extract CloudWatch log messages from the Kinesis payload. These messages are encoded in JSON.
     *
     * @param payloadBytes A Kinesis payload in byte array form.
     * @param objectMapper Jackson object mapper.
     * @return A {@link CloudWatchLogSubscriptionData} instance representing a CloudWatch subscription payload with messages.
     * @throws IOException
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">AWS Subscription Filters</a>
     */
    public static CloudWatchLogSubscriptionData decompressCloudWatchMessages(byte[] payloadBytes, ObjectMapper objectMapper) throws IOException {

        LOG.debug("The supplied payload is GZip compressed. Proceeding to decompress and parse as a CloudWatch log message.");

        final byte[] bytes = Tools.decompressGzip(payloadBytes).getBytes(StandardCharsets.UTF_8);
        LOG.debug("They payload was decompressed successfully. size [{}]", bytes.length);

        final CloudWatchLogSubscriptionData logSubscriptionData = objectMapper.readValue(bytes, CloudWatchLogSubscriptionData.class);

        // The CloudWatch payload often contains many messages. We might need to check how many when debugging throttling issues.
        if (LOG.isTraceEnabled()) {
            LOG.trace("[{}] messages obtained from CloudWatch", logSubscriptionData.logEvents().size());
        }

        return logSubscriptionData;
    }
}
