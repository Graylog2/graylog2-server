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
package org.graylog2.system.processing;

import com.github.joschi.jadconfig.util.Duration;
import com.google.common.util.concurrent.AbstractIdleService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.joda.time.DateTimeZone.UTC;

@Singleton
public class MongoDBProcessingStatusRecorderService extends AbstractIdleService implements ProcessingStatusRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBProcessingStatusRecorderService.class);

    private static final DateTime DEFAULT_RECEIVE_TIME = new DateTime(0L, UTC);

    private final AtomicReference<DateTime> ingestReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);
    private final AtomicReference<DateTime> postProcessingReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);
    private final AtomicReference<DateTime> postIndexReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);

    private final DBProcessingStatusService dbService;
    private final Duration persistInterval;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> future;

    @Inject
    public MongoDBProcessingStatusRecorderService(DBProcessingStatusService dbService,
                                                  @Named(ProcessingStatusConfig.PERSIST_INTERVAL) Duration persistInterval,
                                                  @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.dbService = dbService;
        this.persistInterval = persistInterval;
        this.scheduler = scheduler;
    }

    @Override
    protected void startUp() {
        LOG.debug("Starting processing status recorder service");
        try {
            dbService.get().ifPresent(processingStatus -> {
                LOG.debug("Loaded persisted processing status: {}", processingStatus);

                // Do not directly set the timestamps on the atomic reference to make sure latestTimestamp() is used.
                // The timestamps could already have been updated once the database call is finished.
                final ProcessingStatusDto.ReceiveTimes receiveTimes = processingStatus.receiveTimes();
                updateIngestReceiveTime(receiveTimes.ingest());
                updatePostProcessingReceiveTime(receiveTimes.postProcessing());
                updatePostIndexingReceiveTime(receiveTimes.postIndexing());
            });
        } catch (Exception e) {
            LOG.error("Couldn't load persisted processing status", e);
        }

        final long interval = persistInterval.toMilliseconds();
        future = scheduler.scheduleWithFixedDelay(this::doPersist, interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() {
        LOG.debug("Shutting down processing status recorder service");
        if (future != null) {
            future.cancel(true);
        }
        doPersist();
    }

    private void doPersist() {
        try {
            final ProcessingStatusDto dto = dbService.save(this);
            LOG.debug("Persisted processing status: {}", dto);
        } catch (Exception e) {
            LOG.error("Couldn't persist processing status", e);
        }
    }

    @Override
    public DateTime getIngestReceiveTime() {
        return ingestReceiveTime.get();
    }

    @Override
    public DateTime getPostProcessingReceiveTime() {
        return postProcessingReceiveTime.get();
    }

    @Override
    public DateTime getPostIndexingReceiveTime() {
        return postIndexReceiveTime.get();
    }

    @Override
    public void updateIngestReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            ingestReceiveTime.updateAndGet(timestamp -> latestTimestamp(timestamp, newTimestamp));
        }
    }

    @Override
    public void updatePostProcessingReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            postProcessingReceiveTime.updateAndGet(timestamp -> latestTimestamp(timestamp, newTimestamp));
        }
    }

    @Override
    public void updatePostIndexingReceiveTime(DateTime newTimestamp) {
        if (newTimestamp != null) {
            postIndexReceiveTime.updateAndGet(timestamp -> latestTimestamp(timestamp, newTimestamp));
        }
    }

    private DateTime latestTimestamp(DateTime timestamp, DateTime newTimestamp) {
        return newTimestamp.isAfter(timestamp) ? newTimestamp : timestamp;
    }
}
