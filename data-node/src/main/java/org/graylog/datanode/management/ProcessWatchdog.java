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
import org.graylog.datanode.process.FailuresCounter;
import org.graylog.datanode.process.ProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProcessWatchdog implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessWatchdog.class);
    public static final int WATCHDOG_DEFAULT_DELAY_MS = 1_000;

    private final ManagableProcess process;
    private final int watchdogDelayMs;

    private final FailuresCounter failureCounter = FailuresCounter.zeroBased(3);

    private final ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("process-watchdog-%d").build());
    private volatile boolean stopped;

    public ProcessWatchdog(ManagableProcess process) {
        this(process, WATCHDOG_DEFAULT_DELAY_MS);
    }

    public ProcessWatchdog(ManagableProcess process, int watchdogDelayMs) {
        this.process = process;
        this.watchdogDelayMs = watchdogDelayMs;
    }

    @Override
    public void run() {
        // TODO:  remove busy-waiting and replace with some post-termination hook of the process
        while (!Thread.interrupted() && !stopped) {
            if (process.isInState(ProcessState.TERMINATED)) {
                if (!failureCounter.failedTooManyTimes()) {
                    restartProcess();
                } else {
                    // give up trying, stop the watchdog
                    LOG.warn("Process watchdog terminated after too many restart attempts");
                    stopped = true;
                }
            }

            try {
                Thread.sleep(this.watchdogDelayMs);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
        stopped = true;
        LOG.warn("Process watchdog terminated");
    }

    private void restartProcess() {
        try {
            LOG.info("Detected terminated process, restarting");
            process.start();
        } catch (IOException e) {
            LOG.warn("Failed to start the process.", e);
        } finally {
            failureCounter.increment();
        }
    }

    public void start() {
        LOG.info("Starting watchdog for process");
        stopped = false;
        executor.submit(this);
    }

    public void stop() {
        LOG.info("Stopping watchdog for process");
        stopped = true;
        executor.shutdownNow();
    }

    public boolean isStopped() {
        return stopped;
    }
}
