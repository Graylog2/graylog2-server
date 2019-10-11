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

/**
 * This gets thrown when a precondition for an event processor is not ready. The event processor should be retried
 * at a later point in time.
 */
public class EventProcessorPreconditionException extends EventProcessorException {
    public EventProcessorPreconditionException(String message, String eventDefinitionId) {
        super(message, false, eventDefinitionId);
    }

    public EventProcessorPreconditionException(String message, EventDefinition eventDefinition) {
        super(message, false, eventDefinition);
    }

    public EventProcessorPreconditionException(String message, String eventDefinitionId, EventDefinition eventDefinition) {
        super(message, false, eventDefinitionId, eventDefinition);
    }

    public EventProcessorPreconditionException(String message, String EventDefinitionId, EventDefinition eventDefinition, Throwable cause) {
        super(message, false, EventDefinitionId, eventDefinition, cause);
    }

    public EventProcessorPreconditionException(String message, EventDefinition eventDefinition, Throwable cause) {
        super(message, false, eventDefinition, cause);
    }
}
