package org.graylog.events.notifications;

public class TemporaryEventNotificationException extends EventNotificationException {
    public TemporaryEventNotificationException() {
        super();
    }

    public TemporaryEventNotificationException(String msg) {
        super(msg);
    }

    public TemporaryEventNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
