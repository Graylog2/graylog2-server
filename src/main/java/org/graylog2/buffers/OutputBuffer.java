/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
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
 *
 */

package org.graylog2.buffers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import org.graylog2.GraylogServer;
import org.graylog2.buffers.processors.OutputBufferProcessor;
import org.graylog2.logmessage.LogMessage;
import org.graylog2.ThreadPool;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class OutputBuffer {

    protected ExecutorService executor = new ThreadPool(OutputBuffer.class.getName(), 16, 15000*10);

    GraylogServer server;
    OutputBufferProcessor processor;

    public OutputBuffer(GraylogServer server) {
        this.server = server;
        this.processor = new OutputBufferProcessor(this.server);
    }

    public void initialize() {
    }

    public void insert(LogMessage message) {
        final LogMessageEvent event = new LogMessageEvent();
        event.setMessage(message);
        executor.execute(new Runnable() {
            public void run() {
                try {
                    processor.onEvent(event, 0, false);
                } catch (Exception e) {
                }
            }
        });
    }

}
