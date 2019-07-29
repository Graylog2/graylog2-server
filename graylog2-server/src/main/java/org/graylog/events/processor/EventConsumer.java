package org.graylog.events.processor;

/**
 * Represents an operation that accepts a single input argument and returns no result.
 * <p>
 * The main difference to {@link java.util.function.Consumer} is that {@link #accept(Object)} declares to throw a
 * {@link EventProcessorException}.
 *
 * @param <T> the type of the input to the operation
 */
public interface EventConsumer<T> {
    /**
     * Performs this operation on the given argument.
     *
     * @param events the input
     * @throws EventProcessorException thrown when event processing fails
     */
    void accept(T events) throws EventProcessorException;
}
