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
