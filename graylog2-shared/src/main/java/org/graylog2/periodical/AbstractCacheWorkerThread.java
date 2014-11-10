/**
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
 */
package org.graylog2.periodical;

import com.codahale.metrics.Meter;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.inputs.Cache;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.buffers.ProcessingDisabledException;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class AbstractCacheWorkerThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCacheWorkerThread.class);
    private final ServerStatus serverStatus;
    protected Meter writtenMessages;
    protected Meter outOfCapacity;
    private final int batchLimit;

    protected AbstractCacheWorkerThread(ServerStatus serverStatus, BaseConfiguration configuration) {
        this.serverStatus = serverStatus;
        this.batchLimit = configuration.getRingSize()/2;
    }

    protected void work(Cache cache, Buffer targetBuffer) {
        String cacheName = cache.getClass().getCanonicalName();

        while(true) {
            try {
                //singleMessageEnqueue(cache, targetBuffer);
                drainMessagesEnqueue(cache, targetBuffer);
            } catch (BufferOutOfCapacityException ex) {
                outOfCapacity.mark();
                LOG.error("Target buffer out of capacity in {}. Breaking.", cacheName);
            } catch (ProcessingDisabledException e) {
                LOG.error("Processing has been disabled while working on cache: ", e);
            } catch (Exception e) {
                LOG.error("Exception while working on cache: ", e);
            }
        }
    }

    protected void drainMessagesEnqueue(Cache cache, Buffer targetBuffer) throws BufferOutOfCapacityException, ProcessingDisabledException {
        List<Message> messages = new ArrayList<>();

        Message topElement = null;
        while (topElement == null) {
            topElement = cache.pop();
        }

        messages.add(topElement);
        int result = cache.drainTo(messages, batchLimit);
        //LOG.error("Drained {} messages from cache. Remaining: {}", result, cache.size());
        while (!targetBuffer.hasCapacity(result))
            LockSupport.parkNanos(10);
        targetBuffer.insertCached(messages);
        writtenMessages.mark(messages.size());
    }

    protected void singleMessageEnqueue(Cache cache, Buffer targetBuffer) throws BufferOutOfCapacityException, ProcessingDisabledException {
        Message msg = cache.pop();
        while(!targetBuffer.hasCapacity())
            LockSupport.parkNanos(1000);
        targetBuffer.insertFailFast(msg, msg.getSourceInput());
        writtenMessages.mark();
    }

    @Override
    protected Logger getLogger() {
        return LOG;
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
