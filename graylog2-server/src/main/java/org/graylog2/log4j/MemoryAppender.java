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

package org.graylog2.log4j;

import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * A Log4J appender that keeps a configurable number of messages in memory. Used to make recent internal log messages
 * available via the REST API.
 */
public class MemoryAppender extends AppenderSkeleton {

    private CircularFifoBuffer buffer;
    private int bufferSize;

    @Override
    public void append(LoggingEvent e) {
        buffer.add(e);
    }

    @Override
    public void close() {
        buffer.clear();
    }

    @Override
    public void activateOptions() {
        this.buffer = new CircularFifoBuffer(bufferSize);
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<LoggingEvent> getLogMessages(int max) {
        if (buffer == null) {
            throw new IllegalStateException("Cannot return log messages: Appender is not initialized.");
        }

        final List<LoggingEvent> result = new ArrayList<>(max);
        final Object[] messages = buffer.toArray();
        for(int i = messages.length - 1; i >= 0 && i >= messages.length - max; i--) {
            result.add((LoggingEvent) messages[i]);
        }

        return result;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        checkArgument(bufferSize >= 0);

        this.bufferSize = bufferSize;
    }

}
