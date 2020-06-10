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
     * Only start this thread on primary nodes?
     *
     * @return
     */
    public abstract boolean primaryOnly();

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
