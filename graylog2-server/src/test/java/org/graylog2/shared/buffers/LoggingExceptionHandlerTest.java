/**
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

import org.slf4j.Logger;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.fail;

public class LoggingExceptionHandlerTest {

    @Test
    public void testHandleEventException() throws Exception {
        final Logger logger = mock(Logger.class);
        try {
            final LoggingExceptionHandler handler = new LoggingExceptionHandler(logger);
            handler.handleEventException(new RuntimeException(), -1, null);
            handler.handleEventException(new RuntimeException(), -1, new Object() {
                @Override
                public String toString() {
                    throw new NullPointerException();
                }
            });
        } catch (Exception e) {
            fail("handleEventException should never throw", e);
        }
    }
}