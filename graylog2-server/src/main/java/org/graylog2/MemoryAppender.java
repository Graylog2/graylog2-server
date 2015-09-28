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

import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.collections.iterators.ReverseListIterator;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * A Log4J appender that keeps a configurable number of messages in memory. Used to make recent internal log messages
 * available via the REST API.
 */
public class MemoryAppender extends AppenderSkeleton {

    private CircularFifoBuffer buffer;
    private int bufferSize;

    @Override
    protected void append(LoggingEvent e) {
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

        List<LoggingEvent> messages = Lists.newArrayList();
        ReverseListIterator iter = new ReverseListIterator(new ArrayList<LoggingEvent>(Arrays.asList(buffer.toArray())));
        int i = 0;
        while(iter.hasNext()) {
            messages.add((LoggingEvent) iter.next());
            if (i >= max-1) {
                break;
            }

            i++;
        }

        return messages;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

}
