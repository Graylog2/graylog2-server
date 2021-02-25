/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.system.processing;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.github.joschi.jadconfig.util.Duration;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.codahale.metrics.MetricRegistry.name;
import static org.joda.time.DateTimeZone.UTC;

@Singleton
public class MongoDBProcessingStatusRecorderService extends AbstractIdleService implements ProcessingStatusRecorder {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBProcessingStatusRecorderService.class);

    private static final DateTime DEFAULT_RECEIVE_TIME = new DateTime(0L, UTC);
    private static final String READ_MESSAGES_METRIC = name(LocalKafkaJournal.class.getName(),
            LocalKafkaJournal.METER_READ_MESSAGES);
    private static final String WRITTEN_MESSAGES_METRIC = name(LocalKafkaJournal.class.getName(),
            LocalKafkaJournal.METER_WRITTEN_MESSAGES);
    private static final String UNCOMMITTED_MESSAGES_METRIC = name(LocalKafkaJournal.class.getName(),
            LocalKafkaJournal.GAUGE_UNCOMMITTED_MESSAGES);

    private final AtomicReference<DateTime> ingestReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);
    private final AtomicReference<DateTime> postProcessingReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);
    private final AtomicReference<DateTime> postIndexReceiveTime = new AtomicReference<>(DEFAULT_RECEIVE_TIME);

    private final DBProcessingStatusService dbService;
    private final EventBus eventBus;
    private final ServerStatus serverStatus;
    private final MetricRegistry metricRegistry;
    private final Duration persistInterval;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean inShutdown = new AtomicBoolean(false);
    private ScheduledFuture<?> future;

    @Inject
    public MongoDBProcessingStatusRecorderService(DBProcessingStatusService dbService,
                                                  EventBus eventBus,
                                                  ServerStatus serverStatus,
                                                  MetricRegistry metricRegistry,
                                                  @Named(ProcessingStatusConfig.PERSIST_INTERVAL) Duration persistInterval,
                                                  @Named("daemonScheduler") ScheduledExecutorService scheduler) {
        this.dbService = dbService;
        this.eventBus = eventBus;
        this.serverStatus = serverStatus;
        this.metricRegistry = metricRegistry;
        this.persistInterval = persistInterval;
        this.scheduler = scheduler;
    }

    @Subscribe
    public void handleServerShutdown(Lifecycle lifecycle) {
        if (lifecycle == Lifecycle.HALTING) {
            inShutdown.set(true);
        }
    }

    @Override
    protected void startUp() {
        eventBus.register(this);

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

        inShutdown.set(true);
        eventBus.unregister(this);
        if (future != null) {
            future.cancel(true);
        }
    }

    private void doPersist() {
        if (inShutdown.get()) {
            LOG.debug("Not persisting data because server is shutting down");
            return;
        }
        try {
            final ProcessingStatusDto dto = dbService.save(this);
            LOG.debug("Persisted processing status: {}", dto);
        } catch (Exception e) {
            LOG.error("Couldn't persist processing status", e);
        }
    }

    @Override
    public Lifecycle getNodeLifecycleStatus() {
        return serverStatus.getLifecycle();
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
    public long getJournalInfoUncommittedEntries() {
        //noinspection unchecked
        final Gauge<Long> gauge = (Gauge<Long>) metricRegistry
                .getGauges((name, metric) -> UNCOMMITTED_MESSAGES_METRIC.equals(name))
                .get(UNCOMMITTED_MESSAGES_METRIC);
        if (gauge != null) {
            return gauge.getValue();
        }
        return 0;
    }

    @Override
    public double getJournalInfoReadMessages1mRate() {
        return getJournalInfoMeter1mRate(READ_MESSAGES_METRIC);
    }

    @Override
    public double getJournalInfoWrittenMessages1mRate() {
        return getJournalInfoMeter1mRate(WRITTEN_MESSAGES_METRIC);
    }

    private double getJournalInfoMeter1mRate(String metricName) {
        final Meter meter = metricRegistry.getMeters((name, metric) -> metricName.equals(name)).get(metricName);
        if (meter != null) {
            return meter.getOneMinuteRate();
        }
        return 0;
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
