package org.graylog.events.processor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.graylog.events.fields.EventFieldSpec;
import org.graylog.events.notifications.EventNotificationHandler;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.processor.storage.EventStorageHandler;

public interface EventDefinition {
    String id();

    String title();

    String description();

    int priority();

    boolean alert();

    EventProcessorConfig config();

    ImmutableMap<String, EventFieldSpec> fieldSpec();

    ImmutableList<String> keySpec();

    EventNotificationSettings notificationSettings();

    ImmutableList<EventNotificationHandler.Config> notifications();

    ImmutableList<EventStorageHandler.Config> storage();
}
