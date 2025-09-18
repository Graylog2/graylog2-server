package org.graylog.aws.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.inputs.cloudtrail.json.CloudtrailWriteNotification;
import org.graylog.aws.json.SQSMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SNSNotificationParser {
    private final ObjectMapper objectMapper;
    private static final Logger LOG = LoggerFactory.getLogger(SNSNotificationParser.class);
    private static final String CLOUD_TRAIL_VALIDATION_MESSAGE = "CloudTrail validation message.";

    public SNSNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<SNSNotification> parse(Message message) {

        LOG.debug("Parsing message.");
        try {
            LOG.debug("Reading message body {}.", message.body());
            final SQSMessage envelope = objectMapper.readValue(message.body(), SQSMessage.class);

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
            final List<SNSNotification> notifications = new ArrayList<>(s3ObjectKeys.size());
            for (String s3ObjectKey : s3ObjectKeys) {
                notifications.add(new SNSNotification(message.receiptHandle(), notification.s3Bucket, s3ObjectKey));
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
