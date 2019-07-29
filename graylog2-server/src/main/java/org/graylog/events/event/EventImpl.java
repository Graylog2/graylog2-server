/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.events.event;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.logging.log4j.util.Strings;
import org.graylog.events.fields.FieldValue;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventImpl implements Event {
    private final String eventId;
    private final String eventDefinitionType;
    private final String eventDefinitionId;

    private String originContext;
    private DateTime eventTimestamp;
    private DateTime processingTimestamp;
    private DateTime timerangeStart;
    private DateTime timerangeEnd;
    private ImmutableSet<String> streams = ImmutableSet.of();
    private String message;
    private String source;
    private ImmutableList<String> keyTuple = ImmutableList.of();
    private long priority;
    private boolean alert;
    private Map<String, FieldValue> fields = new HashMap<>();

    EventImpl(String eventId,
              DateTime eventTimestamp,
              String eventDefinitionType,
              String eventDefinitionId,
              String message,
              String source,
              long priority,
              boolean alert) {
        this.eventId = eventId;
        this.eventTimestamp = eventTimestamp;
        this.processingTimestamp = DateTime.now(DateTimeZone.UTC);
        this.eventDefinitionType = eventDefinitionType;
        this.eventDefinitionId = eventDefinitionId;
        this.priority = priority;
        this.alert = alert;
        this.message = message;
        this.source = source;
    }

    @Override
    public String getId() {
        return eventId;
    }

    @Override
    public String getEventDefinitionType() {
        return eventDefinitionType;
    }

    @Override
    public String getEventDefinitionId() {
        return eventDefinitionId;
    }

    @Override
    public String getOriginContext() {
        return originContext;
    }

    @Override
    public void setOriginContext(String originContext) {
        this.originContext = originContext;
    }

    @Override
    public DateTime getEventTimestamp() {
        return eventTimestamp;
    }

    @Override
    public void setEventTimestamp(DateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    @Override
    public DateTime getProcessingTimestamp() {
        return processingTimestamp;
    }

    @Override
    public void setProcessingTimestamp(DateTime processingTimestamp) {
        this.processingTimestamp = processingTimestamp;
    }

    @Override
    public DateTime getTimerangeStart() {
        return timerangeStart;
    }

    @Override
    public void setTimerangeStart(DateTime timerangeStart) {
        this.timerangeStart = timerangeStart;
    }

    @Override
    public DateTime getTimerangeEnd() {
        return timerangeEnd;
    }

    @Override
    public void setTimerangeEnd(DateTime timerangeEnd) {
        this.timerangeEnd = timerangeEnd;
    }

    @Override
    public ImmutableSet<String> getStreams() {
        return streams;
    }

    @Override
    public void addStream(String stream) {
        this.streams = ImmutableSet.<String>builder()
                .addAll(streams)
                .add(stream)
                .build();
    }

    @Override
    public void removeStream(String stream) {
        this.streams = ImmutableSet.<String>builder()
                .addAll(streams.stream().filter(s -> !s.equals(stream)).collect(Collectors.toSet()))
                .build();
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public ImmutableList<String> getKeyTuple() {
        return keyTuple;
    }

    @Override
    public void setKeyTuple(List<String> keyTuple) {
        this.keyTuple = ImmutableList.copyOf(keyTuple);
    }

    @Override
    public long getPriority() {
        return priority;
    }

    @Override
    public void setPriority(long priority) {
        this.priority = priority;
    }

    @Override
    public boolean getAlert() {
        return alert;
    }

    @Override
    public void setAlert(boolean alert) {
        this.alert = alert;
    }

    @Override
    public FieldValue getField(String name) {
        return fields.get(name);
    }

    @Override
    public void setField(String name, FieldValue value) {
        this.fields.put(name, value);
    }

    @Override
    public void setFields(Map<String, String> fields) {
        this.fields = fields.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> FieldValue.string(entry.getValue())));
    }

    @Override
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    @Override
    public EventDto toDto() {
        final Map<String, String> fields = this.fields.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isError())
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().value()));

        return EventDto.builder()
                .id(getId())
                .eventDefinitionType(getEventDefinitionType())
                .eventDefinitionId(getEventDefinitionId())
                .originContext(getOriginContext())
                .eventTimestamp(getEventTimestamp())
                .processingTimestamp(getProcessingTimestamp())
                .timerangeStart(getTimerangeStart())
                .timerangeEnd(getTimerangeEnd())
                .streams(getStreams())
                .message(getMessage())
                .source(getSource())
                .keyTuple(getKeyTuple())
                .key(Strings.join(getKeyTuple(), '|'))
                .priority(getPriority())
                .alert(getAlert())
                .fields(ImmutableMap.copyOf(fields))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventImpl event = (EventImpl) o;
        return priority == event.priority &&
                alert == event.alert &&
                Objects.equals(eventId, event.eventId) &&
                Objects.equals(eventDefinitionType, event.eventDefinitionType) &&
                Objects.equals(eventDefinitionId, event.eventDefinitionId) &&
                Objects.equals(originContext, event.originContext) &&
                Objects.equals(eventTimestamp, event.eventTimestamp) &&
                Objects.equals(processingTimestamp, event.processingTimestamp) &&
                Objects.equals(timerangeStart, event.timerangeStart) &&
                Objects.equals(timerangeEnd, event.timerangeEnd) &&
                Objects.equals(streams, event.streams) &&
                Objects.equals(message, event.message) &&
                Objects.equals(source, event.source) &&
                Objects.equals(keyTuple, event.keyTuple) &&
                Objects.equals(fields, event.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventDefinitionType, eventDefinitionId, originContext, eventTimestamp, processingTimestamp, timerangeStart, timerangeEnd, streams, message, source, keyTuple, priority, alert, fields);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventId", eventId)
                .add("eventDefinitionType", eventDefinitionType)
                .add("eventDefinitionId", eventDefinitionId)
                .add("originContext", originContext)
                .add("eventTimestamp", eventTimestamp)
                .add("processingTimestamp", processingTimestamp)
                .add("timerangeStart", timerangeStart)
                .add("timerangeEnd", timerangeEnd)
                .add("streams", streams)
                .add("message", message)
                .add("source", source)
                .add("keyTuple", keyTuple)
                .add("priority", priority)
                .add("alert", alert)
                .add("fields", fields)
                .toString();
    }
}
