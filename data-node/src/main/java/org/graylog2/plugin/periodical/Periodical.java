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
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public abstract class Periodical implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Periodical.class);

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
     * Only start this thread on leader nodes?
     *
     * @deprecated Use {@link #leaderOnly()} instead.
     */
    @Deprecated
    public boolean masterOnly() {
        return false;
    }

    /**
     * Determines if this periodical should run only on the leader node.
     *
     * @return {@code false} (default) if this periodical may run on every node. {@code true} if it may run only on the
     * leader node
     */
    public boolean leaderOnly() {
        // Defaulting to the now deprecated original method to not break existing implementations.
        return masterOnly();
    }

    /**
     * Start on this node? Useful to decide if to start the periodical based on local configuration.
     *
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
     * @return Seconds to wait before starting the thread. 0 for runsForever() threads.
     */
    public abstract int getInitialDelaySeconds();

    /**
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
            final Logger logger = getLogger();
            if (logger != null) {
                logger.error("Uncaught exception in Periodical", e);
            } else {
                LOG.error("Uncaught exception in Periodical", e);
            }
        }
    }

    @Nonnull
    protected abstract Logger getLogger();

    public abstract void doRun();

    public int getParallelism() {
        return 1;
    }
}
