package org.graylog.events.fields.providers;

import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Map;

public abstract class FieldValueProviderTest {
    protected Message newMessage(Map<String, Object> fields) {
        final Message message = new Message("test message", "test", DateTime.now(DateTimeZone.UTC));
        message.addFields(fields);
        return message;
    }
}
