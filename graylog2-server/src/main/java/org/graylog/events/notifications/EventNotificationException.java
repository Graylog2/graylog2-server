package org.graylog.events.notifications;

public class EventNotificationException extends Exception {
    public EventNotificationException() {
        super();
    }

    public EventNotificationException(String msg) {
        super(msg);
    }

    public EventNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
