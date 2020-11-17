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
package org.graylog2.streams;

import com.google.common.collect.Maps;
import org.graylog2.Configuration;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class StreamFaultManager {
    private static final Logger LOG = LoggerFactory.getLogger(StreamFaultManager.class);

    private final StreamMetrics streamMetrics;
    private final NotificationService notificationService;
    private final StreamService streamService;
    private final int maxFaultCount;
    private final long streamProcessingTimeout;

    private final ConcurrentMap<String, AtomicInteger> faultCounter = Maps.newConcurrentMap();

    @Inject
    public StreamFaultManager(final Configuration configuration,
                              final StreamMetrics streamMetrics,
                              final NotificationService notificationService,
                              final StreamService streamService) {
        this.streamMetrics = streamMetrics;
        this.notificationService = notificationService;
        this.streamService = streamService;
        this.maxFaultCount = configuration.getStreamProcessingMaxFaults();
        this.streamProcessingTimeout = configuration.getStreamProcessingTimeout();
    }

    public long getStreamProcessingTimeout() {
        return streamProcessingTimeout;
    }

    public void registerFailure(final Stream stream) {
        final AtomicInteger faultCount = getFaultCount(stream);
        final int streamFaultCount = faultCount.incrementAndGet();

        streamMetrics.markStreamRuleTimeout(stream.getId());

        if (maxFaultCount > 0 && streamFaultCount >= maxFaultCount) {
            try {
                streamService.pause(stream);
                faultCount.set(0);
                streamMetrics.markStreamFaultsExceeded(stream.getId());
                LOG.error("Processing of stream <{}> failed to return within {}ms for more than {} times. Disabling stream.",
                        stream.getId(), streamProcessingTimeout, maxFaultCount);

                triggerNotification(stream, streamFaultCount);
            } catch (ValidationException ex) {
                LOG.error("Unable to pause stream: {}", ex);
            }
        } else {
            LOG.warn("Processing of stream <{}> failed to return within {}ms.", stream.getId(), streamProcessingTimeout);
        }
    }

    private void triggerNotification(final Stream stream, final int streamFaultCount) {
        final Notification notification = notificationService.buildNow()
                .addType(Notification.Type.STREAM_PROCESSING_DISABLED)
                .addSeverity(Notification.Severity.URGENT)
                .addDetail("stream_id", stream.getId())
                .addDetail("stream_title", stream.getTitle())
                .addDetail("fault_count", streamFaultCount);

        notificationService.publishIfFirst(notification);
    }

    private AtomicInteger getFaultCount(final Stream stream) {
        faultCounter.putIfAbsent(stream.getId(), new AtomicInteger());
        return faultCounter.get(stream.getId());
    }

}
