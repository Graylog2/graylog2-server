package org.graylog.aws.notifications;

public class SNSNotification {
    private final String s3Bucket;
    private final String s3ObjectKey;
    private final String receiptHandle;

    protected SNSNotification(String receiptHandle, String s3Bucket, String s3ObjectKey) {
        this.receiptHandle = receiptHandle;
        this.s3Bucket = s3Bucket;
        this.s3ObjectKey = s3ObjectKey;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

}
