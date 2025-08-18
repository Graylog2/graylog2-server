package org.graylog.events.procedures;

import org.graylog.events.event.EventDto;
import org.graylog.events.processor.EventDefinitionDto;

import java.util.Optional;

public interface EventProcedureProvider {
    default Optional<EventProcedure> get(String procedureId) {
        return Optional.empty();
    }

    default EventProcedure getDecoratedForEvent(EventDefinitionDto eventDefinitionDto, EventDto event) {
        return null;
    }
}
