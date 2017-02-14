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
package org.graylog2.shared.buffers;

import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;

public class LoggingExceptionHandler implements ExceptionHandler<Object> {
    private final Logger logger;

    public LoggingExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
        String eventStr;
        try {
            eventStr = event.toString();
        } catch (Exception e) {
            eventStr = "<invalid>";
        }
        logger.warn("Unable to process event " + eventStr + ", sequence " + sequence, ex);
    }

    @Override
    public void handleOnStartException(Throwable ex) {
        logger.error("Exception occurred while starting disruptor.", ex);
    }

    @Override
    public void handleOnShutdownException(Throwable ex) {
        logger.error("Exception occurred while stopping disruptor.", ex);
    }
}
