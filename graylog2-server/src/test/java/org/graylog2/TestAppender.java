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
package org.graylog2;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.List;

/**
 * Appender which buffers all log statements, useful for testing that some class under test did actually log information.
 */
public class TestAppender extends AppenderSkeleton {

    private final List<LoggingEvent> events = Lists.newArrayList();

    @Override
    protected void append(LoggingEvent event) {
        events.add(event);
    }

    @Override
    public void close() {
        events.clear();
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

    public List<LoggingEvent> getEvents() {
        return ImmutableList.copyOf(events);
    }
}
