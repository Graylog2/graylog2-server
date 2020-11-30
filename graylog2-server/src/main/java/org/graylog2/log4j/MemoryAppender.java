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
package org.graylog2.log4j;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
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

    private Buffer buffer;
    private int bufferSize;

    protected MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, int bufferSize) {
        super(name, filter, layout, ignoreExceptions);
        this.bufferSize = bufferSize;
        this.buffer = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(bufferSize));
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
