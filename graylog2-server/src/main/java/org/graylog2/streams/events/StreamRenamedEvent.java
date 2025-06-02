package org.graylog2.streams.events;

public record StreamRenamedEvent(String streamId, String oldStreamTitle, String newStreamTitle) {
}
