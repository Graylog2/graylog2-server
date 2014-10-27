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
package org.graylog2.caches;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Configuration;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.plugin.Message;
import org.graylog2.utilities.MessageToJsonSerializer;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements a {@link org.graylog2.inputs.Cache} based on MapDB.
 */
public abstract class DiskJournalCache implements InputCache, OutputCache {
    protected static final Logger LOG = LoggerFactory.getLogger(DiskJournalCache.class);

    private final DB db;
    private final BlockingQueue<byte[]> queue;
    private final Atomic.Long counter;
    private final ScheduledExecutorService commitService;
    private final MessageToJsonSerializer serializer;
    private final Store store;
    private final MetricRegistry metricRegistry;
    private final Timer addTimer;
    private final Timer popTimer;
    private final Timer commitTimer;
    private final Timer compactTimer;
    private final Counter lostMessagesCounter;
    private final Counter commitErrorCounter;
    private final Counter compactionErrorCounter;

    public static class Input extends DiskJournalCache {
        @Inject
        public Input(Configuration config, MessageToJsonSerializer serializer, MetricRegistry metricRegistry) throws IOException, DiskJournalCacheCorruptSpoolException {
            super(config, serializer, metricRegistry);
        }

        @Override
        protected String getDbFileName() {
            return "input-cache";
        }
    }

    public static class Output extends DiskJournalCache {
        @Inject
        public Output(Configuration config, MessageToJsonSerializer serializer, MetricRegistry metricRegistry) throws IOException, DiskJournalCacheCorruptSpoolException {
            super(config, serializer, metricRegistry);
        }

        @Override
        protected String getDbFileName() {
            return "output-cache";
        }
    }

    @Inject
    public DiskJournalCache(final Configuration config, final MessageToJsonSerializer serializer, final MetricRegistry metricRegistry) throws IOException, DiskJournalCacheCorruptSpoolException {
        // Ensure the spool directory exists.
        Files.createDirectories(new File(config.getMessageCacheSpoolDir()).toPath());

        this.metricRegistry = metricRegistry;
        try {
            final DBMaker dbMaker = DBMaker.newFileDB(getDbFile(config))
                    .mmapFileEnable()
                    .checksumEnable()
                    .closeOnJvmShutdown();

            if (config.isMessageCacheEnableCompression()) {
                LOG.debug("Enabling compression for disk-based cache \"{}\"", getDbFileName());
                dbMaker.compressionEnable();
            }

            final Size maxSize = config.getMessageCacheMaxSize();
            if (maxSize != null) {
                LOG.debug("Enabling size limit of {} for disk-based cache \"{}\"", maxSize, getDbFileName());
                // Y U NO TAKE BYTES?!
                final double maxSizeGb = maxSize.toBytes() / (1024d * 1024d * 1024d);
                dbMaker.sizeLimit(maxSizeGb);
            }

            this.db = dbMaker.make();
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Caught exception during disk journal initialization: ", e);
            throw new DiskJournalCacheCorruptSpoolException();
        }
        this.store = Store.forDB(this.db);
        this.queue = db.getQueue("messages");
        this.counter = db.getAtomicLong("counter");
        this.commitService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("disk-journal-" + getDbFileName() + "-%d").build()
        );
        this.serializer = serializer;

        this.addTimer = this.metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "add", "executionTime"));
        this.popTimer = this.metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "pop", "executionTime"));
        this.commitTimer = this.metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "commit", "executionTime"));
        this.compactTimer = this.metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "compact", "executionTime"));
        this.lostMessagesCounter = this.metricRegistry.counter(MetricRegistry.name(getClass(), getDbFileName(), "lostMessages"));
        this.commitErrorCounter = this.metricRegistry.counter(MetricRegistry.name(getClass(), getDbFileName(), "commitErrors"));
        this.compactionErrorCounter = this.metricRegistry.counter(MetricRegistry.name(getClass(), getDbFileName(), "compactionErrors"));

        /* Commit and compact the database to flush existing data in the transaction log and to reduce the file
         * size of the database.
         */
        commit();
        compact();

        /* I have seen the counter getting out of sync with the actual entries in the queue. */
        if (queue.isEmpty() && counter.get() != 0) {
            LOG.warn("Setting counter from {} to 0 because the queue is empty!", counter.get());
            counter.set(0);
            commit();
            compact();
        }

        this.commitService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    commit();
                } catch (Exception e) {
                    LOG.error("Commit thread error", e);
                }
            }
        }, 0, config.getMessageCacheCommitInterval(), TimeUnit.MILLISECONDS);


        if (config.getMessageCacheCompactionInterval() != null) {
            LOG.warn("MapDB does not have smart defragmentation algorithms. So compaction usually recreates entire store from scratch.\n"
                    + "This may be slow and require additional disk space.\n"
                    + "You might want to disable the message_cache_compact_interval setting in your Graylog2 configuration");
            this.commitService.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    try {
                        compact();
                    } catch (Exception e) {
                        LOG.error("Compact thread error", e);
                    }
                }
            }, 0, config.getMessageCacheCompactionInterval(), TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("unused")
    @Override
    public void add(final Message message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding message to cache: {}", message.toString());
        }

        if (db.isClosed()) {
            LOG.debug(getDbFileName() + " is closed.");
            lostMessagesCounter.inc();
            return;
        }

        try (final Timer.Context time = addTimer.time()) {
            if (queue.offer(serializer.serializeToBytes(message))) {
                long current = counter.incrementAndGet();
            } else {
                LOG.info("Couldn't add message to disk-based cache \"{}\" (free {}/{})", getDbFileName(), store.getFreeSize(), store.getSizeLimit());
                lostMessagesCounter.inc();
            }
        } catch (IOError e) {
            // Really, MapDB. Really?!
            if (e.getCause() instanceof IOException) {
                LOG.warn("No free space left in disk-based cache \"{}\"", getDbFileName());
            } else {
                LOG.error("Unable to enqueue message", e);
            }

            lostMessagesCounter.inc();
        } catch (Exception e) {
            LOG.error("Unable to enqueue message", e);
            lostMessagesCounter.inc();
        }
    }

    @SuppressWarnings("unused")
    @Override
    public Message pop() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Consuming message from cache");
        }

        if (db.isClosed()) {
            LOG.debug(getDbFileName() + " is closed.");
            lostMessagesCounter.inc();
            return null;
        }

        try (final Timer.Context time = popTimer.time()) {
            final byte[] bytes = queue.poll();

            if (bytes != null) {
                counter.decrementAndGet();
                try {
                    return serializer.deserialize(bytes);
                } catch (IOException e) {
                    LOG.error("Error deserializing message", e);
                    lostMessagesCounter.inc();
                    return null;
                }
            } else {
                return null;
            }
        } catch (IOError e) {
            // Really, MapDB. Really?!
            if (e.getCause() instanceof IOException) {
                LOG.warn("No free space left in disk-based cache \"{}\"", getDbFileName());
            } else {
                LOG.error("Error retrieving message from disk-based cache", e);
            }
            lostMessagesCounter.inc();

            return null;
        } catch (Exception e) {
            LOG.error("Error retrieving message from disk-based cache", e);
            lostMessagesCounter.inc();

            return null;
        }
    }

    @Override
    public int size() {
        if (db.isClosed()) {
            return 0;
        } else {
            return counter.intValue();
        }
    }

    @Override
    public boolean isEmpty() {
        try {
            return db.isClosed() || queue.isEmpty();
        } catch (Exception e) {
            LOG.error("Unexpected error while checking if cache \"" + getDbFileName() + "\"is empty", e);
            return true;
        }
    }

    @SuppressWarnings("unused")
    private void commit() {
        if (db.isClosed()) {
            LOG.debug(getDbFileName() + " is closed. Not committing");
            return;
        }

        try (final Timer.Context time = commitTimer.time()) {
            LOG.debug("Committing {} (entries {})", getDbFileName(), size());
            db.commit();

            if (LOG.isDebugEnabled()) {
                printStats();
            }
        } catch (Exception e) {
            LOG.error("Unexpected error while committing to " + getDbFileName(), e);
            commitErrorCounter.inc();
        }
    }

    @SuppressWarnings("unused")
    private void compact() {
        if (db.isClosed()) {
            LOG.debug(getDbFileName() + " is closed. Not compacting.");
            return;
        }

        try (final Timer.Context time = compactTimer.time()) {
            final long currSize = store.getCurrSize();
            db.compact();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Compacted db {} (freed up {} bytes)", getDbFileName(), (currSize - store.getCurrSize()));
                printStats();
            }
        } catch (Exception e) {
            LOG.error("Unexpected error while compacting " + getDbFileName(), e);
            compactionErrorCounter.inc();
        }
    }

    private File getDbFile(final Configuration config) {
        return new File(config.getMessageCacheSpoolDir(), getDbFileName()).getAbsoluteFile();
    }

    private void printStats() {
        LOG.debug("STATS for \"{}\": counter {}, store size: {} bytes, store free size: {} bytes, store size limit: {} bytes",
                getDbFileName(), counter, store.getCurrSize(), store.getFreeSize(), store.getSizeLimit());
    }

    protected abstract String getDbFileName();
}
