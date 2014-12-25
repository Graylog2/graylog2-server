/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.buffers;

import com.lmax.disruptor.ExceptionHandler;
import org.slf4j.Logger;

public class LoggingExceptionHandler implements ExceptionHandler {
    private Logger logger;

    public LoggingExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void handleEventException(Throwable ex, long sequence, Object event) {
        logger.warn("Unable to process event " + event.toString() + "sequence " + sequence, ex);
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
