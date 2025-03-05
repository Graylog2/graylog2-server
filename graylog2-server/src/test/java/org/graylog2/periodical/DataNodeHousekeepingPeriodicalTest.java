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

import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataNodeHousekeepingPeriodicalTest {

    @Mock
    NodeService<DataNodeDto> nodeService;
    @Mock
    NotificationService notificationService;

    DataNodeHousekeepingPeriodical periodical;

    @BeforeEach
    void setUp() {
        periodical = new DataNodeHousekeepingPeriodical(nodeService, notificationService);
    }

    @Test
    void testDropOutdatedCalled() {
        periodical.doRun();
        verify(nodeService).dropOutdated();
        verifyNoInteractions(notificationService);
    }

    @Test
    void testVersionMismatchNotificationOnMismatch() {
        DataNodeDto sameVersion = mock(DataNodeDto.class);
        when(sameVersion.isCompatibleWithVersion()).thenReturn(true);
        DataNodeDto differentVersion = mock(DataNodeDto.class);
        when(differentVersion.isCompatibleWithVersion()).thenReturn(false);
        when(nodeService.allActive()).thenReturn(Map.of("node1", sameVersion, "node2", differentVersion));
        Notification notification = mock(Notification.class);
        when(notification.addType(any())).thenReturn(notification);
        when(notificationService.buildNow()).thenReturn(notification);
        periodical.doRun();
        verify(notification).addType(Notification.Type.DATA_NODE_VERSION_MISMATCH);
        verify(notification).addSeverity(Notification.Severity.NORMAL);
        verify(notificationService).publishIfFirst(any());
    }

    @Test
    void testNoVersionMismatchNotificationOnMatch() {
        DataNodeDto sameVersion = mock(DataNodeDto.class);
        when(sameVersion.isCompatibleWithVersion()).thenReturn(true);
        DataNodeDto sameVersion2 = mock(DataNodeDto.class);
        when(sameVersion2.isCompatibleWithVersion()).thenReturn(true);
        when(nodeService.allActive()).thenReturn(Map.of("node1", sameVersion, "node2", sameVersion2));
        periodical.doRun();
        verifyNoInteractions(notificationService);
    }


}
