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

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.rolling.FileSize;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.Booleans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * A Log4J appender that keeps a configurable number of messages in memory. Used to make recent internal log messages
 * available via the REST API.
 */
@Plugin(name = "Memory", category = "Core", elementType = "appender", printObject = true)
public class MemoryAppender extends AbstractAppender {
    private static final Logger LOG = LoggerFactory.getLogger(MemoryAppender.class);

    private static final long DEFAULT_BUFFER_SIZE = 50 * 1024 * 1024;
    private final MemoryLimitedCompressingFifoRingBuffer buffer;

    protected MemoryAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions, long bufferSize) {
        super(name, filter, layout, ignoreExceptions, Property.EMPTY_ARRAY);
        buffer = new MemoryLimitedCompressingFifoRingBuffer(bufferSize);
    }

    @PluginFactory
    public static MemoryAppender createAppender(
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "bufferSize") final String bufSizeLegacy,
            @PluginAttribute(value = "bufferSizeBytes", defaultString = "50MB") final String bufferSizeBytes,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final String ignore) {
        if (name == null) {
            LOGGER.error("No name provided for MemoryAppender");
            return null;
        }
        if (bufSizeLegacy != null) {
            LOGGER.error("Deprecated log4j.xml setting detected <{}=\"{}\"> Using default <bufferSizeBytes={}> instead", "bufferSize", bufSizeLegacy, bufferSizeBytes);
        }
        final long bufferSize = bufferSizeBytes == null ? DEFAULT_BUFFER_SIZE : FileSize.parse(bufferSizeBytes, DEFAULT_BUFFER_SIZE);

        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }

        final boolean ignoreExceptions = Booleans.parseBoolean(ignore, true);
        return new MemoryAppender(name, filter, layout, ignoreExceptions, bufferSize);
    }


    @Override
    public void append(LogEvent event) {
        try {
            buffer.add(serializeLogMessage(event.toImmutable())); // only the immutable copy retains potential throwables
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        super.stop();
        try {
            buffer.clear();
        } catch (IOException e) {
            LOG.error("Failed to clear in-mem logs buffer", e);
        }
    }

    private byte[] serializeLogMessage(LogEvent log) {
        return getLayout().toByteArray(log);
    }

    public void streamFormattedLogMessages(OutputStream outputStream, int limit) {
        buffer.streamContent(outputStream, limit);
    }

    public long getLogsSize() {
        return buffer.getLogsSize();
    }
}
