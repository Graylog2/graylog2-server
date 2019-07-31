package org.graylog.scheduler.eventbus;

/**
 * A simple event that signals a scheduler job completion to subscribers.
 * We always use the same instance because there are no fields and it allows us to avoid excessive object creation.
 */
public class JobCompletedEvent {
    public static final JobCompletedEvent INSTANCE = new JobCompletedEvent();

    private JobCompletedEvent() {
    }
}
