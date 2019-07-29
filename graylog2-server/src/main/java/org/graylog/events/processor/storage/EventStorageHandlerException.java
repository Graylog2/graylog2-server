package org.graylog.events.processor.storage;

import static java.util.Objects.requireNonNull;

/**
 * This indicates an error in an {@link EventStorageHandler}.
 */
public class EventStorageHandlerException extends Exception {
    private final EventStorageHandler.Config handlerConfig;

    public EventStorageHandlerException(String message, EventStorageHandler.Config handlerConfig) {
        super(message);
        this.handlerConfig = requireNonNull(handlerConfig, "handlerConfig cannot be null");
    }

    public EventStorageHandlerException(String message, EventStorageHandler.Config handlerConfig, Throwable cause) {
        super(message, cause);
        this.handlerConfig = requireNonNull(handlerConfig, "handlerConfig cannot be null");
    }

    /**
     * Returns the {@link EventStorageHandler.Config} for the storage handler that failed.
     *
     * @return failing storage handler config
     */
    public EventStorageHandler.Config getHandlerConfig() {
        return handlerConfig;
    }
}
