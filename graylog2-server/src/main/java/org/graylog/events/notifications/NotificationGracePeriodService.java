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
package org.graylog.events.notifications;

import com.google.auto.value.AutoValue;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.graylog.events.event.Event;
import org.graylog.events.processor.EventDefinition;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * Checks the grace period of events at an early stage, to prevent creating unnecessary JobTriggers.
 * This is an additional filter to {@link org.graylog.events.notifications.DBNotificationGracePeriodService}
 */
@Singleton
public class NotificationGracePeriodService {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationGracePeriodService.class);

    // This is only used to limit the memory usage
    private static final Duration CACHE_EXPIRY = Duration.ofMinutes(5);

    @AutoValue
    public static abstract class CacheKey {
        public abstract String eventDefinitionId();

        public abstract String notificationId();

        public abstract String eventKey();

        public static CacheKey create(String eventDefinitionId, String notificationId, String eventKey) {
            return new AutoValue_NotificationGracePeriodService_CacheKey(eventDefinitionId, notificationId, eventKey);
        }
    }

    private final LoadingCache<CacheKey, Optional<DateTime>> seenEvents;

    @Inject
    public NotificationGracePeriodService() {
        seenEvents = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY)
                .build(new CacheLoader<CacheKey, Optional<DateTime>>() {
                    @Override
                    public Optional<DateTime> load(CacheKey key) {
                        return Optional.ofNullable(null);
                    }
                });
    }

    public boolean inGracePeriod(EventDefinition definition, String notificationId, Event event) {
        final long gracePeriodMs = definition.notificationSettings().gracePeriodMs();
        if (gracePeriodMs <= 0) {
            return false;
        }
        final Optional<DateTime> lastEventTime = get(definition.id(), notificationId, event.toDto().key());
        final boolean isInGracePeriod = lastEventTime.map(dateTime -> dateTime.isAfter(event.getEventTimestamp().minus(gracePeriodMs))).orElse(false);
        // Only update the timestamp if we are not within the grace period
        if (!isInGracePeriod) {
            put(definition.id(), notificationId, event.toDto().key(), event.getEventTimestamp());
        }
        return isInGracePeriod;
    }

    private Optional<DateTime> get(String eventDefinitionId, String notificationId, String eventKey) {
        try {
            return seenEvents.get(CacheKey.create(eventDefinitionId, notificationId, eventKey));
        } catch (ExecutionException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            LOG.error("Unable to get seenEvent {}/{}/{} from cache", eventDefinitionId, notificationId, eventKey, rootCause);
            throw new RuntimeException(rootCause);
        }
    }

    private void put(String eventDefinitionId, String notificationId, String eventKey, DateTime time) {
        seenEvents.put(CacheKey.create(eventDefinitionId, notificationId, eventKey), Optional.of(time));
    }
}
