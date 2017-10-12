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
package org.graylog2.shared.journal;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import org.graylog2.cluster.Node;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationImpl;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class KafkaJournalDiskCheckPeriodicalTest {
    private static final DateTime TIME = new DateTime(2017, 10, 11, 0, 0, DateTimeZone.UTC);

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Spy
    private BaseConfiguration baseConfiguration = new BaseConfiguration() {
        @Override
        public String getNodeIdFile() {
            return "";
        }
    };
    @Spy
    private KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();
    @Mock
    private ServerStatus serverStatus;
    @Mock
    private Node node;
    @Mock
    private NotificationService notificationService;
    @Mock
    private InputRegistry inputRegistry;

    @Test
    public void doRun_sets_lifecycle_status_DEAD_if_disk_is_full() throws Exception {
        final File mockJournalDir = mock(File.class);
        when(mockJournalDir.getTotalSpace()).thenReturn(100L);
        when(mockJournalDir.getFreeSpace()).thenReturn(1L);
        when(mockJournalDir.getAbsolutePath()).thenReturn("/foo/bar");
        when(kafkaJournalConfiguration.getMessageJournalDir()).thenReturn(mockJournalDir);
        when(kafkaJournalConfiguration.getMessageJournalCheckDiskFreePercent()).thenReturn(5);
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(TIME));

        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        periodical.run();

        ArgumentCaptor<Notification> notificationArgument = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).publishIfFirst(notificationArgument.capture());
        verify(serverStatus, times(1)).overrideLoadBalancerDead();

        final Notification notification = notificationArgument.getValue();
        assertThat(notification).isNotNull();
        assertThat(notification.getType()).isEqualTo(Notification.Type.JOURNAL_INSUFFICIENT_DISK_SPACE);
        assertThat(notification.getSeverity()).isEqualTo(Notification.Severity.URGENT);
        assertThat(notification.getTimestamp()).isEqualTo(TIME);
        assertThat(notification.getDetail("journal_dir")).isEqualTo("/foo/bar");
        assertThat(notification.getDetail("disk_total_bytes")).isEqualTo(100L);
        assertThat(notification.getDetail("disk_free_bytes")).isEqualTo(1L);
        assertThat(notification.getDetail("disk_free_percent")).isEqualTo(1L);
    }

    @Test
    public void doRun_does_not_stop_local_inputs_if_disk_is_full() throws Exception {
        final File mockJournalDir = mock(File.class);
        when(mockJournalDir.getTotalSpace()).thenReturn(100L);
        when(mockJournalDir.getFreeSpace()).thenReturn(1L);
        when(mockJournalDir.getAbsolutePath()).thenReturn("/foo/bar");
        when(kafkaJournalConfiguration.getMessageJournalDir()).thenReturn(mockJournalDir);
        when(kafkaJournalConfiguration.getMessageJournalCheckDiskFreePercent()).thenReturn(5);
        when(kafkaJournalConfiguration.isMessageJournalCheckStopInputs()).thenReturn(false);
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(TIME));

        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        periodical.run();

        verifyNoMoreInteractions(inputRegistry);
        verify(serverStatus, times(1)).overrideLoadBalancerDead();
    }

    @Test
    public void doRun_stops_local_inputs_if_disk_is_full() throws Exception {
        final File mockJournalDir = mock(File.class);
        when(mockJournalDir.getTotalSpace()).thenReturn(100L);
        when(mockJournalDir.getFreeSpace()).thenReturn(1L);
        when(mockJournalDir.getAbsolutePath()).thenReturn("/foo/bar");
        when(kafkaJournalConfiguration.getMessageJournalDir()).thenReturn(mockJournalDir);
        when(kafkaJournalConfiguration.getMessageJournalCheckDiskFreePercent()).thenReturn(5);
        when(kafkaJournalConfiguration.isMessageJournalCheckStopInputs()).thenReturn(true);

        final EventBus eventBus = new EventBus(this.getClass().getSimpleName());
        final MessageInput localInput = mock(MessageInput.class);
        when(localInput.isGlobal()).thenReturn(false);
        final MessageInput globalInput = mock(MessageInput.class);
        when(globalInput.isGlobal()).thenReturn(true);
        final Set<IOState<MessageInput>> inputs = ImmutableSet.of(
                new IOState<>(eventBus, localInput),
                new IOState<>(eventBus, globalInput)
        );
        when(inputRegistry.getRunningInputs()).thenReturn(inputs);
        when(notificationService.buildNow()).thenReturn(new NotificationImpl().addTimestamp(TIME));

        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        periodical.run();

        verify(serverStatus, times(1)).overrideLoadBalancerDead();
        verify(inputRegistry, times(1)).getRunningInputs();
        verify(inputRegistry, times(1)).stop(localInput);
        verify(inputRegistry, never()).stop(globalInput);
        verifyNoMoreInteractions(inputRegistry);
    }

    @Test
    public void doRun_does_nothing_if_disk_is_free() throws Exception {
        final File mockJournalDir = mock(File.class);
        when(mockJournalDir.getTotalSpace()).thenReturn(100L);
        when(mockJournalDir.getFreeSpace()).thenReturn(50L);
        when(mockJournalDir.getAbsolutePath()).thenReturn("/foo/bar");
        when(kafkaJournalConfiguration.getMessageJournalDir()).thenReturn(mockJournalDir);
        when(kafkaJournalConfiguration.getMessageJournalCheckDiskFreePercent()).thenReturn(5);

        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        periodical.run();

        verifyNoMoreInteractions(serverStatus, notificationService);
    }

    @Test
    public void runsForever() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.runsForever()).isFalse();
    }

    @Test
    public void stopOnGracefulShutdown() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.stopOnGracefulShutdown()).isTrue();
    }

    @Test
    public void masterOnly() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.masterOnly()).isFalse();
    }

    @Test
    public void startOnThisNode() throws Exception {
        when(baseConfiguration.isMessageJournalEnabled()).thenReturn(true);
        when(kafkaJournalConfiguration.isMessageJournalCheckEnabled()).thenReturn(true);
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.startOnThisNode()).isTrue();
    }

    @Test
    public void startOnThisNode_returns_false_if_journal_is_disabled() throws Exception {
        when(baseConfiguration.isMessageJournalEnabled()).thenReturn(false);
        when(kafkaJournalConfiguration.isMessageJournalCheckEnabled()).thenReturn(true);
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.startOnThisNode()).isFalse();
    }

    @Test
    public void startOnThisNode_returns_false_if_check_is_disabled() throws Exception {
        when(baseConfiguration.isMessageJournalEnabled()).thenReturn(true);
        when(kafkaJournalConfiguration.isMessageJournalCheckEnabled()).thenReturn(false);
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.startOnThisNode()).isFalse();
    }

    @Test
    public void isDaemon() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.isDaemon()).isTrue();
    }

    @Test
    public void getInitialDelaySeconds() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.getInitialDelaySeconds()).isEqualTo(60);
    }

    @Test
    public void getPeriodSeconds() throws Exception {
        when(kafkaJournalConfiguration.getMessageJournalCheckInterval()).thenReturn(Duration.standardMinutes(23L));
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.getPeriodSeconds()).isEqualTo(23 * 60);
    }

    @Test
    public void getLogger() throws Exception {
        final KafkaJournalDiskCheckPeriodical periodical = new KafkaJournalDiskCheckPeriodical(baseConfiguration, kafkaJournalConfiguration, serverStatus, node, notificationService, inputRegistry);
        assertThat(periodical.getLogger()).isNotNull();
    }
}