package org.graylog2.periodical;

import com.codahale.metrics.Meter;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.inputs.Cache;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.buffers.Buffer;
import org.graylog2.plugin.buffers.BufferOutOfCapacityException;
import org.graylog2.plugin.periodical.Periodical;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class AbstractCacheWorkerThread extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractCacheWorkerThread.class);
    private final ServerStatus serverStatus;
    protected Meter writtenMessages;
    protected Meter outOfCapacity;

    protected AbstractCacheWorkerThread(ServerStatus serverStatus) {
        this.serverStatus = serverStatus;
    }

    protected void work(Cache cache, Buffer targetBuffer) {
        String cacheName = cache.getClass().getCanonicalName();

        while(true) {
            try {
                if (!cache.isEmpty() && serverStatus.isProcessing()) {
                    LOG.debug("{} contains {} messages. Trying to process them.", cacheName, cache.size());

                    while (true) {
                        if (cache.isEmpty()) {
                            LOG.debug("Read all messages from {}.", cacheName);
                            break;
                        }

                        if (targetBuffer.hasCapacity() && serverStatus.isProcessing()) {
                            try {
                                LOG.debug("Reading message from {}.", cacheName);
                                Message msg = cache.pop();
                                if (msg != null) {
                                    targetBuffer.insertFailFast(msg, msg.getSourceInput());
                                    writtenMessages.mark();
                                }
                            } catch (BufferOutOfCapacityException ex) {
                                outOfCapacity.mark();
                                LOG.debug("Target buffer out of capacity in {}. Breaking.", cacheName);
                                break;
                            }
                        }
                    }
                }
            } catch(Exception e) {
                LOG.error("Error while trying to work on MasterCache <" + cacheName + ">.", e);
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            }

            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
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
