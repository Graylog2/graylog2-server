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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.graylog2.log4j.MemoryAppender;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class MemoryAppenderTest {
    @Test
    public void testGetLogMessages() throws Exception {
        final int bufferSize = 10;
        final MemoryAppender appender = MemoryAppender.createAppender(null, null, "memory", "10", "false");

        for (int i = 1; i <= bufferSize; i++) {
            final LogEvent logEvent = Log4jLogEvent.newBuilder()
                    .setLevel(Level.INFO)
                    .setLoggerName("test")
                    .setLoggerFqcn("com.example.test")
                    .setMessage(new SimpleMessage("Message " + i))
                    .build();


            appender.append(logEvent);
        }

        assertThat(appender.getLogMessages(bufferSize * 2)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize)).hasSize(bufferSize);
        assertThat(appender.getLogMessages(bufferSize / 2)).hasSize(bufferSize / 2);
        assertThat(appender.getLogMessages(0)).isEmpty();

        final List<LogEvent> messages = appender.getLogMessages(5);
        for (int i = 0; i < messages.size(); i++) {
            assertThat(messages.get(i).getMessage().getFormattedMessage()).isEqualTo("Message " + (bufferSize - i));
        }
    }
}