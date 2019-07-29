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
package org.graylog.events.processor;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * This is thrown when an {@link EventProcessor} fails.
 */
public class EventProcessorException extends Exception {
    private final String eventDefinitionId;
    private final boolean permanent;
    private EventDefinition eventDefinition;

    public EventProcessorException(String message, boolean permanent, String eventDefinitionId) {
        super(message);
        this.permanent = permanent;
        this.eventDefinitionId = requireNonNull(eventDefinitionId, "eventDefinitionId cannot be null");
    }

    public EventProcessorException(String message, boolean permanent, EventDefinition eventDefinition) {
        super(message);
        this.permanent = permanent;
        this.eventDefinition = requireNonNull(eventDefinition, "eventDefinition cannot be null");
        this.eventDefinitionId = requireNonNull(eventDefinition.id(), "eventDefinitionId cannot be null");
    }

    public EventProcessorException(String message, boolean permanent, String eventDefinitionId, EventDefinition eventDefinition) {
        super(message);
        this.permanent = permanent;
        this.eventDefinitionId = requireNonNull(eventDefinitionId, "eventDefinitionId cannot be null");
        this.eventDefinition = requireNonNull(eventDefinition, "eventDefinition cannot be null");
    }

    public EventProcessorException(String message, boolean permanent, String eventDefinitionId, EventDefinition eventDefinition, Throwable cause) {
        super(message, cause);
        this.permanent = permanent;
        this.eventDefinitionId = requireNonNull(eventDefinitionId, "eventDefinitionId cannot be null");
        this.eventDefinition = requireNonNull(eventDefinition, "eventDefinition cannot be null");
    }

    public EventProcessorException(String message, boolean permanent, EventDefinition eventDefinition, Throwable cause) {
        super(message, cause);
        this.permanent = permanent;
        this.eventDefinition = requireNonNull(eventDefinition, "eventDefinition cannot be null");
        this.eventDefinitionId = requireNonNull(eventDefinition.id(), "eventDefinitionId cannot be null");
    }

    /**
     * Indicates if an error is permanent or temporary. Temporary errors could be candidates for retries.
     *
     * @return true if the error is permanent, false if temporary
     */
    public boolean isPermanent() {
        return permanent;
    }

    /**
     * Returns the event definition ID that failed to execute.
     *
     * @return the event definition ID
     */
    public String getEventDefinitionId() {
        return eventDefinitionId;
    }

    /**
     * Returns the event definition that failed.
     *
     * @return the event definition for the failed processor or an empty {@link Optional} if absent
     */
    public Optional<EventDefinition> getEventDefinition() {
        return Optional.ofNullable(eventDefinition);
    }
}
