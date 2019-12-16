package org.graylog2.shared.messageq;

public class MessageQueueException extends Exception {
    public MessageQueueException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessageQueueException(String message) {
        super(message);
    }
}
