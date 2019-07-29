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
