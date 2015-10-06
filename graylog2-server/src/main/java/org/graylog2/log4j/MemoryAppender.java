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
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A Log4J appender that keeps a configurable number of messages in memory. Used to make recent internal log messages
 * available via the REST API.
 */
@Plugin(name = "Memory", category = "Core", elementType = "appender", printObject = true)
public class MemoryAppender extends AbstractAppender {

    private CircularFifoBuffer buffer;
    private int bufferSize;

    protected MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, int bufferSize) {
        super(name, filter, layout, ignoreExceptions);
        this.bufferSize = bufferSize;
        this.buffer = new CircularFifoBuffer(bufferSize);
    }

    @PluginFactory
    public static MemoryAppender createAppender(
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "bufferSize", defaultInt = 500) final String bufferSize,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for MemoryAppender");
            return null;
        }

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        final int size = Integer.parseInt(bufferSize);
        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        return new MemoryAppender(name, filter, layout, ignoreExceptions, size);
    }


    @Override
    public void append(LogEvent event) {
        buffer.add(event);
    }

    @Override
    public void stop() {
        super.stop();
        buffer.clear();
    }

    public List<LogEvent> getLogMessages(int max) {
        if (buffer == null) {
            throw new IllegalStateException("Cannot return log messages: Appender is not initialized.");
        }

        final List<LogEvent> result = new ArrayList<>(max);
        final Object[] messages = buffer.toArray();
        for (int i = messages.length - 1; i >= 0 && i >= messages.length - max; i--) {
            result.add((LogEvent) messages[i]);
        }

        return result;
    }

    public int getBufferSize() {
        return bufferSize;
    }
}
