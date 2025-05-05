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

import com.github.joschi.jadconfig.util.Duration;
import org.graylog2.configuration.SearchIndexerHosts;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationServiceImpl;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.security.jwt.IndexerJwtAuthToken;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.versionprobe.VersionProbe;
import org.graylog2.storage.versionprobe.VersionProbeFactory;
import org.graylog2.storage.versionprobe.VersionProbeListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchVersionCheckPeriodicalTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        this.notificationService = mock(NotificationServiceImpl.class);
        when(this.notificationService.build()).thenCallRealMethod();
        when(this.notificationService.buildNow()).thenCallRealMethod();
    }

    @Test
    void doesNotRunIfVersionOverrideIsSet() {
        final SearchVersion initialVersion = SearchVersion.elasticsearch(8, 0, 0);
        final SearchVersion overrideVersion = SearchVersion.elasticsearch(7, 0, 0);
        createPeriodical(
                initialVersion,
                overrideVersion,
                mockVersionProbeFactory(initialVersion)).doRun();

        verifyNoInteractions(notificationService);
    }

    @Test
    void doesNotDoAnythingIfVersionWasNotProbed() {
        final SearchVersion initialVersion = SearchVersion.elasticsearch(8, 0, 0);
        createPeriodical(initialVersion, mockVersionProbeFactory(null)).doRun();
        verifyNoInteractions(notificationService);
    }

    @Test
    void createsNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        final SearchVersion initialVersion = SearchVersion.elasticsearch(8, 1, 2);
        final VersionProbeFactory versionProbe = mockVersionProbeFactory(SearchVersion.elasticsearch(9, 2, 3));
        createPeriodical(initialVersion, versionProbe).doRun();

        assertNotificationWasRaised();
    }

    @Test
    void createsNotificationIfCurrentVersionIncompatiblyOlderThanInitialOne() {
        final SearchVersion initialVersion = SearchVersion.elasticsearch(8, 1, 2);
        final VersionProbeFactory versionProbe = mockVersionProbeFactory(SearchVersion.elasticsearch(6, 8, 1));
        createPeriodical(initialVersion, versionProbe).doRun();

        assertNotificationWasRaised();
    }

    @Test
    void fixesNotificationIfCurrentVersionIsIncompatibleWithInitialOne() {
        final SearchVersion initialVersion = SearchVersion.elasticsearch(8, 1, 2);
        VersionProbeFactory versionProbe = mockVersionProbeFactory(SearchVersion.elasticsearch(8, 2, 3));
        createPeriodical(initialVersion, versionProbe).doRun();

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

    private Periodical createPeriodical(SearchVersion initialVersion, VersionProbeFactory versionProbeFactory) {
        return createPeriodical(initialVersion, null, versionProbeFactory);
    }

    private Periodical createPeriodical(SearchVersion initialVersion, @Nullable SearchVersion versionOverride, VersionProbeFactory versionProbe) {
        return new SearchVersionCheckPeriodical(
                initialVersion,
                versionOverride,
                versionProbe,
                IndexerJwtAuthToken.disabled(),
                notificationService,
                ()  -> new SearchIndexerHosts(Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );
    }

    private VersionProbeFactory mockVersionProbeFactory(SearchVersion expectedResult) {
        return new VersionProbeFactory() {
            @Override
            public VersionProbe createDefault() {
                return (hosts) -> Optional.ofNullable(expectedResult);
            }

            @Override
            public VersionProbe create(IndexerJwtAuthToken jwtAuthToken, int probeAttempts, Duration probeDelay, VersionProbeListener versionProbeListener) {
                return createDefault();
            }
        };
    }
}
