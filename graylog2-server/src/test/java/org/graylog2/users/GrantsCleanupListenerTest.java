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
package org.graylog2.users;

import com.google.common.eventbus.EventBus;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.users.events.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GrantsCleanupListenerTest {

    @Mock
    DBGrantService grantService;

    @Mock
    PaginatedUserService userService;

    GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();
    GrantsCleanupListener cleanupListener;

    private final UserOverviewDTO userA = UserOverviewDTO.builder()
            .id("a")
            .username("a@graylog.local")
            .email("a@graylog.local")
            .fullName("a")
            .build();

    private final GrantDTO grantUserA = GrantDTO.of(
            grnRegistry.newGRN(GRNTypes.USER, "a"), Capability.VIEW, grnRegistry.newGRN(GRNTypes.DASHBOARD, "d"));
    private final GrantDTO grantUserB = GrantDTO.of(
            grnRegistry.newGRN(GRNTypes.USER, "b"), Capability.VIEW, grnRegistry.newGRN(GRNTypes.DASHBOARD, "d"));
    private final GrantDTO grantTeam = GrantDTO.of(
            grnRegistry.newGRN(GRNTypes.TEAM, "t"), Capability.VIEW, grnRegistry.newGRN(GRNTypes.DASHBOARD, "d"));

    @BeforeEach
    void setUp() {
        cleanupListener = new GrantsCleanupListener(mock(EventBus.class), grantService, userService, grnRegistry);
    }

    @Test
    void noGrants() {
        when(userService.streamAll()).thenReturn(Stream.of(userA));
        when(grantService.streamAll()).thenReturn(Stream.empty());

        cleanupListener.handleUserDeletedEvent(mock(UserDeletedEvent.class));

        verify(grantService, never()).deleteForGrantee(any());
    }

    @Test
    void userRemoved() {
        when(userService.streamAll()).thenReturn(Stream.of(userA));
        when(grantService.streamAll()).thenReturn(Stream.of(grantUserA, grantUserB, grantTeam));

        cleanupListener.handleUserDeletedEvent(mock(UserDeletedEvent.class));

        verify(grantService).deleteForGrantee(grnRegistry.newGRN(GRNTypes.USER, "b"));
        verifyNoMoreInteractions(grantService);
    }
}
