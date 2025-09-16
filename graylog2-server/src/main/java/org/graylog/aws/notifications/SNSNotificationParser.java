package org.graylog.aws.notifications;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.aws.sqs.ObjectCreatedPutParseException;
import org.graylog.aws.sqs.S3Bucket;
import org.graylog.aws.sqs.SQSMessage;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SNSNotificationParser {
    private final ObjectMapper objectMapper;
    private static final Logger LOG = LoggerFactory.getLogger(SNSNotificationParser.class);

    public SNSNotificationParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Parse all possible SNS notification payloads formats.
     * {
     * Records arrays are commonly used for standard S3 bucket notifications:
     * "Records":[
     * {
     * ...
     * "s3":{
     * "s3SchemaVersion":"1.0",
     * "configurationId":"ID found in the bucket notification configuration",
     * "bucket":{
     * "name":"amzn-s3-demo-bucket",
     * "ownerIdentity":{
     * "principalId":"Amazon-customer-ID-of-the-bucket-owner"
     * },
     * "arn":"bucket-ARN"
     * },
     * "object":{
     * "key":"object-key",
     * "size":"object-size in bytes",
     * "eTag":"object eTag",
     * "versionId":"object version if bucket is versioning-enabled, otherwise null",
     * "sequencer": "a string representation of a hexadecimal value used to determine event sequence, only used with PUTs and DELETEs"
     * }
     * },
     * "glacierEventData": {
     * "restoreEventData": {
     * "lifecycleRestorationExpiryTime": "The time, in ISO-8601 format, for example, 1970-01-01T00:00:00.000Z, of Restore Expiry",
     * "lifecycleRestoreStorageClass": "Source storage class for restore"
     * }
     * }
     * }
     * ],
     * Detail elements are commonly used in Security Lake notifications:
     * "detail": {
     * "bucket": {
     * "name": "amzn-s3-demo-bucket"
     * },
     * "object": {
     * "key": "example-key",
     * "size": 5,
     * "etag": "b57f9512698f4b09e608f4f2a65852e5"
     * },
     * "request-id": "N4N7GDK58NMKJ12R",
     * "requester": "securitylake.amazonaws.com"
     * }
     * }
     */
    public List<SNSNotification> parse(Message message) {
        List<SNSNotification> notifications = new ArrayList<>();
        SQSMessage envelope = new SQSMessage();
        try {
            envelope = objectMapper.readValue(message.body(), SQSMessage.class);
        } catch (IOException e) {
            LOG.error("Parsing exception. [{}]", ExceptionUtils.getRootCauseMessage(e));
        }
        if (envelope.records == null && envelope.detail == null) {
            LOG.warn("The SQS message does not contain valid S3 ObjectCreated:Put event so it cannot be processed. Skipping message....");
            throw new ObjectCreatedPutParseException(message.receiptHandle(), "The SQS message does not contain valid S3 ObjectCreated:Put event so it cannot be processed. Skipping message....");
        }
        if (envelope.records != null) {
            envelope.records.stream()
                    .map(record -> record.get("s3"))
                    .filter(Objects::nonNull)
                    .forEach(s3Node -> {
                        try {
                            S3Bucket notification = objectMapper.readValue(s3Node.toString(), S3Bucket.class);
                            notifications.add(new SNSNotification(message.receiptHandle(),
                                    notification.bucket().get("name").asText(), notification.object().get("key").asText()));
                        } catch (IOException e) {
                            LOG.error("Parsing exception. [{}]", ExceptionUtils.getRootCauseMessage(e));
                        }
                    });
        }
        if (envelope.detail != null) {
            try {
                S3Bucket notification = objectMapper.readValue(envelope.detail.toString(), S3Bucket.class);
                notifications.add(new SNSNotification(message.receiptHandle(),
                        notification.bucket().get("name").asText(), notification.object().get("key").asText()));
            } catch (IOException e) {
                LOG.error("Parsing exception. [{}]", ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return notifications;
    }
}
