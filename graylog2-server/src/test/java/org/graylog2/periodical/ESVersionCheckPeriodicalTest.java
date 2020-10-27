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
package org.graylog2.periodical;

import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ESVersionCheckPeriodicalTest {
    private VersionProbe versionProbe;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        this.versionProbe = mock(VersionProbe.class);
        this.notificationService = mock(NotificationServiceImpl.class);
        when(this.notificationService.build()).thenCallRealMethod();
        when(this.notificationService.buildNow()).thenCallRealMethod();
    }

    @Test
    void doesNotRunIfVersionOverrideIsSet() {
        createPeriodical(Version.from(8, 0, 0), Version.from(7, 0, 0)).doRun();

        verifyNoInteractions(notificationService);
    }

    @Test
    void doesNotDoAnythingIfVersionWasNotProbed() {
        returnProbedVersion(null);

        createPeriodical(Version.from(8, 0, 0)).doRun();

        verifyNoInteractions(notificationService);
    }

    @Test
    void createsNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        returnProbedVersion(Version.from(9, 2, 3));

        createPeriodical(Version.from(8, 1, 2)).doRun();

        assertNotificationWasRaised();
    }

    @Test
    void fixesNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        returnProbedVersion(Version.from(8, 2, 3));

        createPeriodical(Version.from(8, 1, 2)).doRun();

        assertNotificationWasFixed();
    }

    private void assertNotificationWasFixed() {
        final ArgumentCaptor<Notification.Type> captor = ArgumentCaptor.forClass(Notification.Type.class);
        verify(notificationService, times(1)).fixed(captor.capture());

        assertThat(captor.getValue()).isEqualTo(Notification.Type.ES_VERSION_MISMATCH);
    }

    private void assertNotificationWasRaised() {
        final ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationService, times(1)).publishIfFirst(captor.capture());

        assertThat(captor.getValue().getType()).isEqualTo(Notification.Type.ES_VERSION_MISMATCH);
    }

    private void returnProbedVersion(@Nullable Version probedVersion) {
        when(versionProbe.probe(anyCollection())).thenReturn(Optional.ofNullable(probedVersion));
    }

    private Periodical createPeriodical(Version initialVersion) {
        return new ESVersionCheckPeriodical(initialVersion, null, Collections.emptyList(), versionProbe, notificationService);
    }

    private Periodical createPeriodical(Version initialVersion, @Nullable Version versionOverride) {
        return new ESVersionCheckPeriodical(initialVersion, versionOverride, Collections.emptyList(), versionProbe, notificationService);
    }
}
