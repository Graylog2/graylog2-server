package org.graylog.events.processor;

import com.google.common.collect.Sets;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;

import java.util.Set;
import java.util.stream.Collectors;

public class EventStreamService {

    private final StreamService streamService;

    private EventStreamService(StreamService streamService) {
        this.streamService = streamService;
    }

    // Determine event source streams based on given search and result streams
    public Set<String> buildEventSourceStreams(Set<String> searchStreams, Set<String> resultSourceStreams) {
        Set<String> sourceStreams;
        if (searchStreams.isEmpty() && resultSourceStreams.isEmpty()) {
            // This can happen if the user didn't select any stream in the event definition and an event should be
            // created based on the absence of a search result. (e.g. count() < 1)
            // When the source streams field of an event is empty, every user can see it. That's why we need to add
            // streams to the field. We decided to use all currently existing streams (minus the default event streams)
            // to make sure only users that have access to all message streams can see the event.
            sourceStreams = streamService.loadAll().stream()
                    .map(Persisted::getId)
                    .filter(streamId -> !Stream.DEFAULT_EVENT_STREAM_IDS.contains(streamId))
                    .collect(Collectors.toSet());
        } else if (searchStreams.isEmpty()) {
            // If the search streams is empty, we search in all streams and so we include all source streams from the result.
            sourceStreams = resultSourceStreams;
        } else if (resultSourceStreams.isEmpty()) {
            // With an empty result we just include all streams from the event definition.
            sourceStreams = searchStreams;
        } else {
            // We don't want source streams in the event which are unrelated to the event definition.
            sourceStreams = Sets.intersection(searchStreams, resultSourceStreams);
        }
        return sourceStreams;
    }
}
