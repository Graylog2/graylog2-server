/*
 * Copyright 2012-2014 TORCH GmbH
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
 */

package org.graylog2.caches;

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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implements a {@link org.graylog2.inputs.Cache} based on MapDB.
 *
 * @author Bernd Ahlers <bernd@torch.sh>
 */
public abstract class DiskJournalCache implements InputCache, OutputCache {
    private final Logger LOG = LoggerFactory.getLogger(DiskJournalCache.class);

    private final DB db;
    private final BlockingQueue<byte[]> queue;
    private final Atomic.Long counter;
    private final ScheduledExecutorService commitService;
    private final MessageToJsonSerializer serializer;
    private final Object modificationLock = new Object();
    private final Store store;
    private final MetricRegistry metricRegistry;
    private final Timer addTimer;
    private final Timer popTimer;
    private final Timer commitTimer;

    public static class Input extends DiskJournalCache {
        @Inject
        public Input(Configuration config, MessageToJsonSerializer serializer, MetricRegistry metricRegistry) throws IOException {
            super(config, serializer, metricRegistry);
        }

        @Override
        protected String getDbFileName() {
            return "input-cache";
        }
    }

    public static class Output extends DiskJournalCache {
        @Inject
        public Output(Configuration config, MessageToJsonSerializer serializer, MetricRegistry metricRegistry) throws IOException {
            super(config, serializer, metricRegistry);
        }

        @Override
        protected String getDbFileName() {
            return "output-cache";
        }
    }

    @Inject
    public DiskJournalCache(final Configuration config, final MessageToJsonSerializer serializer, final MetricRegistry metricRegistry) throws IOException {
        // Ensure the spool directory exists.
        Files.createDirectories(new File(config.getMessageCacheSpoolDir()).toPath());

        this.metricRegistry = metricRegistry;
        this.db = DBMaker.newFileDB(getDbFile(config)).mmapFileEnable().checksumEnable().closeOnJvmShutdown().make();
        this.store = Store.forDB(this.db);
        this.queue = db.getQueue("messages");
        this.counter = db.getAtomicLong("counter");
        this.commitService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("disk-journal-cache-%d").build()
        );
        this.serializer = serializer;
        this.addTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "add", "executionTime"));
        this.popTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "pop", "executionTime"));
        this.commitTimer = metricRegistry.timer(MetricRegistry.name(getClass(), getDbFileName(), "commit", "executionTime"));

        /* Commit and compact the database to flush existing data in the transaction log and to reduce the file
         * size of the database.
         */
        commit();
        compact();

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
    }

    @Override
    public void add(final Message message) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Adding message to cache: {}", message.toString());
        }
        if (db.isClosed()) {
            return;
        }
        final Timer.Context time = addTimer.time();
        try {
            final byte[] bytes = serializer.serializeToBytes(message);

            synchronized (modificationLock) {
                if (queue.offer(bytes)) {
                    counter.incrementAndGet();
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to enqueue message", e);
        } finally {
            time.stop();
        }

    }

    @Override
    public Message pop() {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Consuming message from cache");
        }
        if (db.isClosed()) {
            return null;
        }

        final byte[] bytes;
        final Timer.Context time = popTimer.time();

        synchronized (modificationLock) {
            bytes = queue.poll();

            if (bytes != null) {
                counter.decrementAndGet();
            }
        }

        if (bytes != null) {
            try {
                return serializer.deserialize(bytes);
            } catch (IOException e) {
                LOG.error("Error deserializing message", e);
                return null;
            } finally {
                time.stop();
            }
        } else {
            time.stop();
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
    public void clear() {
        if (db.isClosed()) {
            return;
        }
        LOG.debug("Clearing cache");
        synchronized (modificationLock) {
            queue.clear();
            counter.set(0);
            db.commit();
            db.compact();
        }
    }

    @Override
    public boolean isEmpty() {
        return db.isClosed() || queue.isEmpty();
    }

    private void commit() {
        if (db.isClosed()) {
            return;
        }
        final Timer.Context time = commitTimer.time();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committing {} (entries {})", getDbFileName(), size());
        }
        db.commit();
        time.stop();
    }

    private void compact() {
        if (db.isClosed()) {
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
