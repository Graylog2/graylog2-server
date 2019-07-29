package org.graylog.events.event;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog.events.fields.FieldValue;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Map;

public interface Event {
    String getId();

    String getEventDefinitionType();

    String getEventDefinitionId();

    String getOriginContext();

    void setOriginContext(String originContext);

    DateTime getEventTimestamp();

    void setEventTimestamp(DateTime timestamp);

    DateTime getProcessingTimestamp();

    void setProcessingTimestamp(DateTime processingTimestamp);

    DateTime getTimerangeStart();

    void setTimerangeStart(DateTime timerangeStart);

    DateTime getTimerangeEnd();

    void setTimerangeEnd(DateTime timerangeEnd);

    ImmutableSet<String> getStreams();

    void addStream(String stream);

    void removeStream(String stream);

    String getMessage();

    void setMessage(String message);

    String getSource();

    void setSource(String source);

    ImmutableList<String> getKeyTuple();

    void setKeyTuple(List<String> keyTuple);

    long getPriority();

    void setPriority(long priority);

    boolean getAlert();

    void setAlert(boolean alert);

    FieldValue getField(String name);

    void setField(String name, FieldValue value);

    void setFields(Map<String, String> fields);

    boolean hasField(String name);

    EventDto toDto();

    static Event fromDto(EventDto from) {
        EventImpl event = new EventImpl(from.id(), from.eventTimestamp(), from.eventDefinitionType(), from.eventDefinitionId(), from.message(), from.source(), from.priority(), from.alert());
        event.setProcessingTimestamp(from.processingTimestamp());
        event.setKeyTuple(from.keyTuple());
        from.streams().forEach(event::addStream);
        event.setFields(from.fields());
        event.setPriority(from.priority());

        from.timerangeStart().ifPresent(event::setTimerangeStart);
        from.timerangeEnd().ifPresent(event::setTimerangeEnd);
        from.originContext().ifPresent(event::setOriginContext);

        return event;
    }

}
