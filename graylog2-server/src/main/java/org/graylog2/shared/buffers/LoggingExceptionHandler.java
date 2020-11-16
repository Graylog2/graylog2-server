/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
