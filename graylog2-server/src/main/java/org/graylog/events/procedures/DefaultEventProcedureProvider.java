package org.graylog.events.procedures;

import org.graylog.events.event.EventDto;

import java.util.Optional;

public class DefaultEventProcedureProvider implements EventProcedureProvider {

    @Override
    public Optional<EventProcedure> getDecoratedForEvent(String eventProcedureId, EventDto event) {
        return Optional.empty();
    }
}
