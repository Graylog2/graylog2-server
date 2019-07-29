package org.graylog.events.notifications;

public interface EventNotification {
    interface Factory<TYPE extends EventNotification> {
        TYPE create();
    }

    void execute(EventNotificationContext ctx) throws EventNotificationException;
}
