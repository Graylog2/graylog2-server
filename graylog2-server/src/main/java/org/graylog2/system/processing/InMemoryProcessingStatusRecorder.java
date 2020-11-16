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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AtomicDouble;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.joda.time.DateTime;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.joda.time.DateTimeZone.UTC;

/**
 * This {@link ProcessingStatusRecorder} implementation should only be used for tests.
 */
@Singleton
public class InMemoryProcessingStatusRecorder implements ProcessingStatusRecorder {
    private final AtomicReference<DateTime> ingestReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));
    private final AtomicReference<DateTime> postProcessingReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));
    private final AtomicReference<DateTime> postIndexReceiveTime = new AtomicReference<>(new DateTime(0L, UTC));

    @VisibleForTesting
    final AtomicLong uncommittedMessages = new AtomicLong(0);
    @VisibleForTesting
    final AtomicDouble readMessages1m = new AtomicDouble(0);
    @VisibleForTesting
    final AtomicDouble writtenMessages1m = new AtomicDouble(0);

    @Override
    public Lifecycle getNodeLifecycleStatus() {
        return Lifecycle.RUNNING;
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
        return uncommittedMessages.get();
    }

    @Override
    public double getJournalInfoReadMessages1mRate() {
        return readMessages1m.get();
    }

    @Override
    public double getJournalInfoWrittenMessages1mRate() {
        return writtenMessages1m.get();
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
