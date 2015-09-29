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
package org.graylog2;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryAppenderTest {

    private MemoryAppender appender;

    @Before
    public void setUp() {
        appender = new MemoryAppender();
    }

    @Test
    public void testGetLogMessages() throws Exception {
        final int bufferSize = 10;
        appender.setBufferSize(bufferSize);
        appender.activateOptions();

        for (int i = 1; i <= bufferSize; i++) {
            appender.append(new LoggingEvent("com.example", Logger.getRootLogger(), Level.INFO, "Message " + i, null));
        }

        assertThat(appender.getLogMessages(bufferSize * 2)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize / 2)).hasSize(bufferSize / 2);
        assertThat(appender.getLogMessages(0)).isEmpty();

        final List<LoggingEvent> messages = appender.getLogMessages(5);
        for (int i = 0; i < messages.size(); i++) {
            assertThat(messages.get(i).getMessage()).isEqualTo("Message " + (bufferSize - i));
        }
    }
}