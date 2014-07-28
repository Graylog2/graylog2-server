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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.Configuration;
import org.graylog2.inputs.InputCache;
import org.graylog2.inputs.OutputCache;
import org.graylog2.plugin.Message;
import org.graylog2.utilities.MessageToJsonSerializer;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
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

    public static class Input extends DiskJournalCache {
        @Inject
        public Input(Configuration config, MessageToJsonSerializer serializer) throws IOException {
            super(config, serializer);
        }

        @Override
        protected String getDbFileName() {
            return "input-cache";
        }
    }

    public static class Output extends DiskJournalCache {
        @Inject
        public Output(Configuration config, MessageToJsonSerializer serializer) throws IOException {
            super(config, serializer);
        }

        @Override
        protected String getDbFileName() {
            return "output-cache";
        }
    }

    @Inject
    public DiskJournalCache(final Configuration config, final MessageToJsonSerializer serializer) throws IOException {
        // Ensure the spool directory exists.
        Files.createDirectories(new File(config.getCacheSpoolDir()).toPath());

        this.db = DBMaker.newFileDB(getDbFile(config)).mmapFileEnable().checksumEnable().closeOnJvmShutdown().make();
        this.queue = db.getQueue("messages");
        this.counter = db.getAtomicLong("counter");
        this.commitService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("disk-journal-cache-%d").build()
        );
        this.serializer = serializer;

        this.commitService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                commit();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void add(final Message message) {
        LOG.debug("Adding message to cache: {}", message.toString());
        try {
            synchronized (modificationLock) {
                if (queue.offer(serializer.serializeToBytes(message))) {
                    counter.incrementAndGet();
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to enqueue message", e);
        }

    }

    @Override
    public Message pop() {
        LOG.debug("Consuming message from cache");
        final byte[] bytes;

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
            }
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        return counter.intValue();
    }

    @Override
    public void clear() {
        LOG.debug("Clearing cache");
        queue.clear();
        counter.set(0);
        db.commit();
    }

    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    private void commit() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Committing {} (size {})", getDbFileName(), size());
        }
        db.commit();
    }

    private File getDbFile(final Configuration config) {
        return new File(config.getCacheSpoolDir(), getDbFileName()).getAbsoluteFile();
    }

    protected abstract String getDbFileName();
}
