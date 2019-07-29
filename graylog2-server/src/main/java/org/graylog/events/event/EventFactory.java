package org.graylog.events.event;

import org.graylog.events.processor.EventDefinition;
import org.joda.time.DateTime;

public interface EventFactory {
    Event createEvent(EventDefinition eventDefinition, DateTime eventTime, String message);
}
