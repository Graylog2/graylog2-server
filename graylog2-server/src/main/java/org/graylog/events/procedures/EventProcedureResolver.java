package org.graylog.events.procedures;

import java.util.Optional;

public interface EventProcedureResolver {
    default Optional<EventProcedure> get(String procedureId) {
        return Optional.empty();
    }
}
