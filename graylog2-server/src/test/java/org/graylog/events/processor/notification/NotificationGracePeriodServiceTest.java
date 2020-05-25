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
package org.graylog.events.processor.notification;

import com.google.common.collect.ImmutableList;
import org.graylog.events.event.Event;
import org.graylog.events.event.TestEvent;
import org.graylog.events.notifications.EventNotificationSettings;
import org.graylog.events.notifications.NotificationGracePeriodService;
import org.graylog.events.processor.EventDefinition;
import org.joda.time.DateTime;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;
import static org.mockito.Mockito.when;

public class NotificationGracePeriodServiceTest {

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private EventDefinition definition;
    @Mock
    private EventNotificationSettings settings;

    @Test
    public void falseWithDisabledGracePeriod() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(0L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of("testkey"));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
    }

    @Test
    public void withinGracePeriod() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of("testkey"));
        final Event event2 = new TestEvent();
        event2.setKeyTuple(ImmutableList.of("testkey"));
        event2.setEventTimestamp(event.getEventTimestamp().plus(5L));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isTrue();
    }

    @Test
    public void outsideGracePeriod() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of("testkey"));
        final Event event2 = new TestEvent();
        event2.setKeyTuple(ImmutableList.of("testkey"));
        event2.setEventTimestamp(event.getEventTimestamp().plus(11L));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isFalse();
    }

    @Test
    public void insideThenInsideGracePeriod() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent(DateTime.now(UTC), "testkey");
        final Event event2 = new TestEvent(event.getEventTimestamp().plus(5L), "testkey");
        final Event event3 = new TestEvent(event2.getEventTimestamp().plus(9L), "testkey");

        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isTrue();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event3)).isTrue();
    }

    @Test
    public void insideThenOutsideGracePeriod() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent(DateTime.now(UTC), "testkey");
        final Event event2 = new TestEvent(event.getEventTimestamp().plus(5L), "testkey");
        final Event event3 = new TestEvent(event2.getEventTimestamp().plus(11L), "testkey");

        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isTrue();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event3)).isFalse();
    }

    @Test
    public void differentKey() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of("testkey"));
        final Event event2 = new TestEvent();
        event2.setKeyTuple(ImmutableList.of("otherkey"));
        event2.setEventTimestamp(event.getEventTimestamp().plus(1L));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isFalse();
    }

    @Test
    public void differentNotification() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of("testkey"));
        final Event event2 = new TestEvent();
        event2.setKeyTuple(ImmutableList.of("testkey"));
        event2.setEventTimestamp(event.getEventTimestamp().plus(1L));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "4242", event2)).isFalse();
    }

    @Test
    public void emptyKey() {
        final NotificationGracePeriodService notificationGracePeriodService = new NotificationGracePeriodService();

        when(settings.gracePeriodMs()).thenReturn(10L);
        when(definition.notificationSettings()).thenReturn(settings);
        when(definition.id()).thenReturn("1234");

        final Event event = new TestEvent();
        event.setKeyTuple(ImmutableList.of());
        final Event event2 = new TestEvent();
        event.setKeyTuple(ImmutableList.of());
        event2.setEventTimestamp(event.getEventTimestamp().plus(1L));
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event)).isFalse();
        assertThat(notificationGracePeriodService.inGracePeriod(definition, "5678", event2)).isTrue();
    }
}
