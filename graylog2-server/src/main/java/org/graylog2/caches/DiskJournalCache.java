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

import com.codahale.metrics.InstrumentedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Implements a {@link org.graylog2.inputs.Cache} based on MapDB.
 */
public abstract class DiskJournalCache implements InputCache, OutputCache {
    private static final Logger LOG = LoggerFactory.getLogger(DiskJournalCache.class);

    private final DB db;
    private final BlockingQueue<byte[]> queue;
    private final Atomic.Long counter;
    private final MessageToJsonSerializer serializer;
    private final Store store;
    private final MetricRegistry metricRegistry;
    private final Timer addTimer;
    private final Timer popTimer;
    private final Timer commitTimer;

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
            this.db = DBMaker.newFileDB(getDbFile(config)).mmapFileEnable().checksumEnable().closeOnJvmShutdown().make();
        } catch (ArrayIndexOutOfBoundsException e) {
            LOG.error("Caught exception during disk journal initialization: ", e);
            throw new DiskJournalCacheCorruptSpoolException();
        }
        this.store = Store.forDB(this.db);
        this.queue = db.getQueue("messages");
        this.counter = db.getAtomicLong("counter");
        this.serializer = serializer;
        this.addTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "add", "executionTime"));
        this.popTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "pop", "executionTime"));
        this.commitTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "commit", "executionTime"));

        /* Commit and compact the database to flush existing data in the transaction log and to reduce the file
         * size of the database.
         */
        commit();
        LOG.info("Compacting off-heap message cache database files ({})", getDbFileName());
        compact();

        /* I have seen the counter getting out of sync with the actual entries in the queue. */
        if (queue.isEmpty() && counter.get() != 0) {
            LOG.warn("Setting counter from {} to 0 because the queue is empty!", counter.get());
            counter.set(0);
            commit();
        }

        final ScheduledExecutorService commitService = commitExecutorService();
        commitService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    commit();
                } catch (Exception e) {
                    LOG.error("Commit thread error", e);
                }
            }
        }, 0, config.getMessageCacheCommitInterval(), TimeUnit.MILLISECONDS);
    }

    private ScheduledExecutorService commitExecutorService() {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("disk-journal-" + getDbFileName() + "-%d").build();
        return new InstrumentedScheduledExecutorService(Executors.newSingleThreadScheduledExecutor(threadFactory), metricRegistry);
    }

    @Override
    public void add(final Message message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding message to cache: {}", message.toString());
        }

        if (db.isClosed()) {
            LOG.trace("Database is closed. Not adding message.");
            return;
        }

        try (final Timer.Context time = addTimer.time()) {
            if (queue.offer(serializer.serializeToBytes(message))) {
                counter.incrementAndGet();
            }
        } catch (IOException e) {
            LOG.error("Unable to enqueue message", e);
        } catch (IllegalAccessError e) {
            throw new IllegalStateException("Error while accessing database", e);
        }
    }

    @Override
    public void add(Collection<Message> m) {
        for (Message message : m) {
            add(message);
        }
    }

    @Override
    public Message pop() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Consuming message from cache");
        }

        if (db.isClosed()) {
            LOG.trace("Database is closed. Not consuming message.");
            return null;
        }

        try (final Timer.Context time = popTimer.time()) {
            final byte[] bytes = queue.take();
            if (bytes != null) {
                counter.decrementAndGet();
                return serializer.deserialize(bytes);
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while dequeueing message: ", e);
        } catch (IOException e) {
            LOG.error("Error deserializing message", e);
        } catch (IllegalAccessError e) {
            throw new IllegalStateException("Error while accessing database", e);
        }

        return null;
    }

    @Override
    public int drainTo(Collection<? super Message> c, int n) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Consuming message from cache");
        }

        if (db.isClosed()) {
            LOG.trace("Database is closed. Not consuming message.");
            return 0;
        }

        final Timer.Context time = popTimer.time();
        final List<byte[]> resultList = new ArrayList<>();
        queue.drainTo(resultList, n);

        int result = 0;

        for (byte[] bytes : resultList) {
            if (bytes != null) {
                counter.decrementAndGet();
                try {
                    c.add(serializer.deserialize(bytes));
                    result += 1;
                } catch (IOException e) {
                    LOG.error("Error deserializing message", e);
                }
            }
        }
        time.stop();

        return result;
    }

    @Override
    public int size() {
        if (db.isClosed()) {
            LOG.trace("Database is closed. Not calculating size.");
            return 0;
        } else {
            return counter.intValue();
        }
    }

    @Override
    public boolean isEmpty() {
        return db.isClosed() || queue.isEmpty();
    }

    private void commit() {
        if (db.isClosed()) {
            LOG.trace("Database is closed. Not committing to disk.");
            return;
        }

        try (final Timer.Context time = commitTimer.time()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Committing {} (entries {})", getDbFileName(), size());
            }
            db.commit();
        }
    }

    private void compact() {
        if (db.isClosed()) {
            LOG.trace("Database is closed. Not compacting.");
            return;
        }

        final long currSize = store.getCurrSize();

        db.compact();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Compacted db {} (freed up {} bytes)", getDbFileName(), (currSize - store.getCurrSize()));
        }
    }

    private File getDbFile(final Configuration config) {
        return new File(config.getMessageCacheSpoolDir(), getDbFileName()).getAbsoluteFile();
    }

    protected abstract String getDbFileName();
}
