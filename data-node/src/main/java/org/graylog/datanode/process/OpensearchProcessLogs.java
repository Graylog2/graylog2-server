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
package org.graylog.datanode.process;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class OpensearchProcessLogs implements Closeable {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessLogs.class);

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final long pid;
    private final CircularFifoQueue<String> stdOut;
    private final CircularFifoQueue<String> stdErr;

    public OpensearchProcessLogs(long pid, InputStream stdout, InputStream stderr, int logsBufferSize) {
        this.pid = pid;

        this.stdOut = new CircularFifoQueue<>(logsBufferSize);
        this.stdErr = new CircularFifoQueue<>(logsBufferSize);

        StreamConsumer outputConsumer = new StreamConsumer(stdout, line -> {
            stdOut.offer(line);
            // TODO: logger should include the process ID as well
            LOG.info(line);
        });
        StreamConsumer errorConsumer = new StreamConsumer(stderr, line ->  {
            stdErr.offer(line);
            // TODO: logger should include the process ID as well
            LOG.warn(line);
        });

        executor.submit(outputConsumer);
        executor.submit(errorConsumer);
    }

    public static OpensearchProcessLogs createFor(Process process, int logsBufferSize) {
        return new OpensearchProcessLogs(process.pid(), process.getInputStream(), process.getErrorStream(), logsBufferSize);
    }

    public List<String> getStdOut() {
        return new ArrayList<>(stdOut);
    }


    public List<String> getStdErr() {
        return new ArrayList<>(stdErr);
    }


    @Override
    public void close() {
        try {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
