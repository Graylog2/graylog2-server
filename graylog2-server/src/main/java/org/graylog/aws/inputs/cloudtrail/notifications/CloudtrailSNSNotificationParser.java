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
package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.json.CloudtrailWriteNotification;
import org.graylog.aws.json.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CloudtrailSNSNotificationParser {

    private static final String CLOUD_TRAIL_VALIDATION_MESSAGE = "CloudTrail validation message.";
    private static final Logger LOG = LoggerFactory.getLogger(CloudtrailSNSNotificationParser.class);

    private final ObjectMapper objectMapper;

    public CloudtrailSNSNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<CloudtrailSNSNotification> parse(Message message) {

        LOG.debug("Parsing message.");
        try {
            LOG.debug("Reading message body {}.", message.getBody());
            final SQSMessage envelope = objectMapper.readValue(message.getBody(), SQSMessage.class);

            if (envelope.message == null) {
                LOG.warn("Message is empty. Processing of message has been aborted. Verify that the SQS subscription in AWS is NOT set to send raw data.");
                return Collections.emptyList();
            }

            LOG.debug("Reading message envelope {}.", envelope.message);
            if (envelope.message.contains(CLOUD_TRAIL_VALIDATION_MESSAGE)) {
                return Collections.emptyList();
            }

            final CloudtrailWriteNotification notification = objectMapper.readValue(envelope.message, CloudtrailWriteNotification.class);

            final List<String> s3ObjectKeys = notification.s3ObjectKey;
            if (s3ObjectKeys == null) {
                LOG.debug("No S3 object keys parsed.");
                return Collections.emptyList();
            }

            LOG.debug("Processing [{}] S3 keys.", s3ObjectKeys.size());
            final List<CloudtrailSNSNotification> notifications = new ArrayList<>(s3ObjectKeys.size());
            for (String s3ObjectKey : s3ObjectKeys) {
                notifications.add(new CloudtrailSNSNotification(message.getReceiptHandle(), notification.s3Bucket, s3ObjectKey));
            }

            LOG.debug("Returning [{}] notifications.", notifications.size());
            return notifications;
        } catch (IOException e) {
            LOG.error("Parsing exception.", e);
            /* Don't throw an exception that would halt processing for one parsing failure.
             * Sometimes occasional non-JSON test messages will come through. If this happens,
             * just log the error and keep processing.
              *
              * Returning an empty list here is OK and should be caught by the caller. */
            return new ArrayList<>();
        }
    }
}
