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
package org.graylog.datanode.management;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public class LoggingOutputStream extends OutputStream {

    private final StringBuilder builder = new StringBuilder();
    private final Consumer<String> consumer;

    public LoggingOutputStream(Consumer<String> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void write(int b) throws IOException {
        if (b == '\n') {
            consumeLine(builder.toString());
            builder.setLength(0); // reset the builder
        } else {
            builder.append((char) b);
        }
    }

    private void consumeLine(String line) {
        consumer.accept(line);
    }

    @Override
    public void flush() throws IOException {
        super.flush();
        if(!builder.isEmpty()) {
            consumeLine(builder.toString());
        }
    }
}
