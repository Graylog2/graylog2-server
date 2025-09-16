package org.graylog.aws.sqs;

public class ObjectCreatedPutParseException extends RuntimeException {
    private final String receiptHandle;

    public ObjectCreatedPutParseException(String receiptHandle, String message) {
        super(message);
        this.receiptHandle = receiptHandle;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }
}

