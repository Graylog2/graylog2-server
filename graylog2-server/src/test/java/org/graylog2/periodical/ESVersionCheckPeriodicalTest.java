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
package org.graylog2.periodical;

import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.storage.SearchVersion;
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
        createPeriodical(SearchVersion.elasticsearch(8, 0, 0), SearchVersion.elasticsearch(7, 0, 0)).doRun();

        verifyNoInteractions(notificationService);
    }

    @Test
    void doesNotDoAnythingIfVersionWasNotProbed() {
        returnProbedVersion(null);
        createPeriodical(SearchVersion.elasticsearch(8, 0, 0)).doRun();
        verifyNoInteractions(notificationService);
    }

    @Test
    void createsNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        returnProbedVersion(com.github.zafarkhaja.semver.Version.forIntegers(9, 2, 3));

        createPeriodical(SearchVersion.elasticsearch(8, 1, 2)).doRun();

        assertNotificationWasRaised();
    }

    @Test
    void createsNotificationIfCurrentVersionIncompatiblyOlderThanInitialOne() {
        returnProbedVersion(com.github.zafarkhaja.semver.Version.forIntegers(6, 8, 1));

        createPeriodical(SearchVersion.elasticsearch(8, 1, 2)).doRun();

        assertNotificationWasRaised();
    }

    @Test
    void fixesNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        returnProbedVersion(com.github.zafarkhaja.semver.Version.forIntegers(8, 2, 3));

        createPeriodical(SearchVersion.elasticsearch(8, 1, 2)).doRun();

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

    private void returnProbedVersion(@Nullable com.github.zafarkhaja.semver.Version probedVersion) {
        when(versionProbe.probe(anyCollection())).thenReturn(Optional.ofNullable(probedVersion).map(SearchVersion::elasticsearch));
    }

    private Periodical createPeriodical(SearchVersion initialVersion) {
        return new ESVersionCheckPeriodical(initialVersion, null, Collections.emptyList(), versionProbe, notificationService);
    }

    private Periodical createPeriodical(SearchVersion initialVersion, @Nullable SearchVersion versionOverride) {
        return new ESVersionCheckPeriodical(initialVersion, versionOverride, Collections.emptyList(), versionProbe, notificationService);
    }
}
