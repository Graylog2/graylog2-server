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

import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;

public class LoggingExceptionHandlerTest {
    @Test
    public void testHandleEventException() throws Exception {
        final Logger logger = mock(Logger.class);
        final LoggingExceptionHandler handler = new LoggingExceptionHandler(logger);
        handler.handleEventException(new RuntimeException(), -1, null);
        handler.handleEventException(new RuntimeException(), -1, new Object() {
            @Override
            public String toString() {
                throw new NullPointerException();
            }
        });
    }
}
