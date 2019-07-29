package org.graylog.events.notifications;

public class PermanentEventNotificationException extends EventNotificationException {
    public PermanentEventNotificationException() {
        super();
    }

    public PermanentEventNotificationException(String msg) {
        super(msg);
    }

    public PermanentEventNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
