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
 * This is an additional filter to {@link org.graylog.events.notifications.DBNotificationService}
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
        put(definition.id(), notificationId, event.toDto().key(), event.getEventTimestamp());
        return isInGracePeriod;
    }

    private Optional<DateTime> get(String eventDefinitionId, String notificationId, String eventKey) {
        try {
            return seenEvents.get(new AutoValue_NotificationGracePeriodService_CacheKey(eventDefinitionId, notificationId, eventKey));
        } catch (ExecutionException e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            LOG.error("Unable to get seenEvent {}/{}/{} from cache", eventDefinitionId, notificationId, eventKey, rootCause);
            throw new RuntimeException(rootCause);
        }
    }

    private void put(String eventDefinitionId, String notificationId, String eventKey, DateTime time) {
        seenEvents.put(new AutoValue_NotificationGracePeriodService_CacheKey(eventDefinitionId, notificationId, eventKey), Optional.of(time));
    }
}
