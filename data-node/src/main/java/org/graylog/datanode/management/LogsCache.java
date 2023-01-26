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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.graylog.datanode.process.StreamConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogsCache implements ExecuteStreamHandler, ProcessLogs {

    private static final Logger LOG = LoggerFactory.getLogger(LogsCache.class);

    private final ExecutorService executor;
    private final CircularFifoQueue<String> stdOut;
    private final CircularFifoQueue<String> stdErr;
    private InputStream processErrorStream;
    private InputStream processOutputStream;

    public LogsCache(int logsBufferSize) {
        this.stdOut = new CircularFifoQueue<>(logsBufferSize);
        this.stdErr = new CircularFifoQueue<>(logsBufferSize);
        this.executor = Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("output-logging-%d").build());
    }


    public List<String> stdOut() {
        return new ArrayList<>(stdOut);
    }


    public List<String> stdErr() {
        return new ArrayList<>(stdErr);
    }


    @Override
    public void setProcessInputStream(OutputStream os) {
        // we ignore the input of the process
    }

    @Override
    public void setProcessErrorStream(InputStream is) {
        this.processErrorStream = is;
    }

    @Override
    public void setProcessOutputStream(InputStream is) {
        this.processOutputStream = is;
    }

    @Override
    public void start() throws IOException {
        StreamConsumer outputConsumer = new StreamConsumer(processOutputStream, line -> {
            stdOut.offer(line);
            LOG.info(line);
        });
        StreamConsumer errorConsumer = new StreamConsumer(processErrorStream, line -> {
            stdErr.offer(line);
            LOG.warn(line);
        });

        executor.submit(outputConsumer);
        executor.submit(errorConsumer);
    }

    @Override
    public void stop() throws IOException {
        // consumer threads are blocked by read waiting and can't be cleanly interrupted. Kill them without trying.
        executor.shutdownNow();
    }
}
