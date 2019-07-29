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
