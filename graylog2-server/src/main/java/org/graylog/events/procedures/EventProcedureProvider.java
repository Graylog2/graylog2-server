package org.graylog.events.procedures;

import java.util.Optional;

public interface EventProcedureProvider {
    default Optional<EventProcedure> get(String procedureId) {
        return Optional.empty();
    }
}
