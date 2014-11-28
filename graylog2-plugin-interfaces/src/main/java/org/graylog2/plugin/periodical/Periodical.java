/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
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
