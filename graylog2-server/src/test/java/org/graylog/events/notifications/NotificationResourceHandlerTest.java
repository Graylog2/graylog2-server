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

import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.scheduler.DBJobDefinitionService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class NotificationResourceHandlerTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private NotificationResourceHandler notificationResourceHandler;

    @Mock
    private DBNotificationService dbNotificationService;
    @Mock
    private DBJobDefinitionService jobDefinitionService;
    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private Map<String, EventNotification.Factory> eventNotificationFactories;
    @Mock
    private EventNotification.Factory eventNotificationFactory;
    @Mock
    private EventNotification eventNotification;

    private NotificationDto getHttpNotification() {
        return NotificationDto.builder()
                .title("Foobar")
                .id("1234")
                .description("")
                .config(HTTPEventNotificationConfig.Builder.create()
                        .url("http://localhost")
                        .build())
                .build();
    }

    @Before
    public void setUp() throws Exception {
        when(dbNotificationService.get(anyString())).thenReturn(Optional.of(getHttpNotification()));
        when(eventNotificationFactory.create()).thenReturn(eventNotification);
        when(eventNotificationFactories.get(anyString())).thenReturn(eventNotificationFactory);
        notificationResourceHandler = new NotificationResourceHandler(dbNotificationService, jobDefinitionService, eventDefinitionService, eventNotificationFactories);
    }

    @Test
    public void testExecution() throws EventNotificationException {
        notificationResourceHandler.test(getHttpNotification(), "testUser");

        ArgumentCaptor<EventNotificationContext> captor = ArgumentCaptor.forClass(EventNotificationContext.class);
        verify(eventNotification, times(1)).execute(captor.capture());

        assertThat(captor.getValue()).satisfies(ctx -> {
            assertThat(ctx.event().message()).isEqualTo("Notification test message triggered from user <testUser>");
            assertThat(ctx.notificationId()).isEqualTo(NotificationTestData.TEST_NOTIFICATION_ID);
            assertThat(ctx.notificationConfig().type()).isEqualTo(HTTPEventNotificationConfig.TYPE_NAME);
            assertThat(ctx.eventDefinition().get().title()).isEqualTo("Event Definition Test Title");
        });
    }
}
