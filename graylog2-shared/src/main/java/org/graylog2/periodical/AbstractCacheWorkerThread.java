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

        messages.add(cache.pop());
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
