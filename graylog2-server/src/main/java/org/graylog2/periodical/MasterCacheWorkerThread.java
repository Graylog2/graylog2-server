/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
package org.graylog2.periodical;

import com.codahale.metrics.Meter;
import org.graylog2.inputs.Cache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class MasterCacheWorkerThread extends Periodical {

    private static final Logger LOG = LoggerFactory.getLogger(MasterCacheWorkerThread.class);

    private Meter writtenMessages;
    private Meter outOfCapacity;

    @Override
    public void run() {
        writtenMessages = core.metrics().meter(name(MasterCacheWorkerThread.class, "writtenMessages"));
        outOfCapacity =  core.metrics().meter(name(MasterCacheWorkerThread.class, "FailedWritesOutOfCapacity"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                work(core.getInputCache(), core.getProcessBuffer());
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                work(core.getOutputCache(), core.getOutputBuffer());
            }
        }).start();
    }

    private void work(Cache cache, Buffer targetBuffer) {
        String cacheName = cache.getClass().getCanonicalName();;

        while(true) {
            try {
                if (cache.size() > 0 && core.isProcessing()) {
                    LOG.debug("{} contains {} messages. Trying to process them.", cacheName, cache.size());

                    while (true) {
                        if (cache.size() <= 0) {
                            LOG.debug("Read all messages from {}.", cacheName);
                            break;
                        }

                        if (targetBuffer.hasCapacity() && core.isProcessing()) {
                            try {
                                LOG.debug("Reading message from {}.", cacheName);
                                Message msg = cache.pop();
                                targetBuffer.insertFailFast(msg, msg.getSourceInput());
                                writtenMessages.mark();
                            } catch (BufferOutOfCapacityException ex) {
                                outOfCapacity.mark();
                                LOG.debug("Target buffer out of capacity in {}. Breaking.", cacheName);
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                LOG.error("Error while trying to work on MasterCache <{}>.", cacheName, e);
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) { /* */ }
            }

            try {
                Thread.sleep(100);
            } catch(InterruptedException ex) { /* */ }
        }
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }
}
