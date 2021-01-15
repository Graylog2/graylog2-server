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
package org.graylog2.plugin.periodical;

import org.slf4j.Logger;

public abstract class Periodical implements Runnable {

    /**
     * Defines if this thread should be called periodically or only once
     * on startup.
     *
     * @return
     */
    public abstract boolean runsForever();

    /**
     * Should this thread be stopped when a graceful shutdown is in progress?
     * This means that stop() is called and that is no longer triggered periodically.
     *
     * @return
     */
    public abstract boolean stopOnGracefulShutdown();

    /**
     * Only start this thread on master nodes?
     *
     * @return
     */
    public abstract boolean masterOnly();

    /**
     * Start on this node? Useful to decide if to start the periodical based on local configuration.
     * @return
     */
    public abstract boolean startOnThisNode();

    /**
     * Should this periodical be run as a daemon thread?
     *
     * @return
     */
    public abstract boolean isDaemon();

    /**
     *
     * @return Seconds to wait before starting the thread. 0 for runsForever() threads.
     */
    public abstract int getInitialDelaySeconds();

    /**
     *
     * @return How long to wait between each execution of the thread. 0 for runsForever() threads.
     */
    public abstract int getPeriodSeconds();

    public void initialize() {
    }

    @Override
    public void run() {
        try {
            doRun();
        } catch (RuntimeException e) {
            getLogger().error("Uncaught exception in periodical", e);
        }
    }

    protected abstract Logger getLogger();

    public abstract void doRun();

    public int getParallelism() {
        return 1;
    }
}
