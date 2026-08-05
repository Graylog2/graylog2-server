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
package org.graylog.plugins.views.startpage.recentActivities;

import com.google.common.eventbus.EventBus;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.events.EntitySharesUpdateEvent;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecentActivityUpdatesListenerTest {
    private final GRNRegistry grnRegistry = GRNRegistry.createWithBuiltinTypes();

    @Mock
    private RecentActivityService recentActivityService;
    @Mock
    private GRNDescriptorService grnDescriptorService;
    @Mock
    private User user;

    private RecentActivityUpdatesListener listener;

    @BeforeEach
    void setUp() {
        listener = new RecentActivityUpdatesListener(new EventBus(), recentActivityService, grnDescriptorService);
        when(user.getFullName()).thenReturn("Jane Doe");
    }

    @Test
    void sharingAnEntityStoresItsResolvedTitle() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        final GRN grantee = grnRegistry.newGRN(GRNTypes.USER, "jane");
        when(grnDescriptorService.getDescriptor(entity)).thenReturn(GRNDescriptor.create(entity, "My Collection"));

        listener.createRecentActivityFor(EntitySharesUpdateEvent.create(user, entity,
                List.of(EntitySharesUpdateEvent.Share.create(grantee, Capability.VIEW, null)),
                List.of(), List.of()));

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService).save(captor.capture());
        assertThat(captor.getValue()).satisfies(dto -> {
            assertThat(dto.activityType()).isEqualTo(ActivityType.SHARE);
            assertThat(dto.itemGrn()).isEqualTo(entity);
            assertThat(dto.itemTitle()).isEqualTo("My Collection");
            assertThat(dto.userName()).isEqualTo("Jane Doe");
            assertThat(dto.grantee()).isEqualTo(grantee.toString());
        });
    }

    @Test
    void unsharingAnEntityStoresItsResolvedTitle() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        final GRN grantee = grnRegistry.newGRN(GRNTypes.USER, "jane");
        when(grnDescriptorService.getDescriptor(entity)).thenReturn(GRNDescriptor.create(entity, "My Collection"));

        listener.createRecentActivityFor(EntitySharesUpdateEvent.create(user, entity,
                List.of(),
                List.of(EntitySharesUpdateEvent.Share.create(grantee, Capability.VIEW, null)),
                List.of()));

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService).save(captor.capture());
        assertThat(captor.getValue()).satisfies(dto -> {
            assertThat(dto.activityType()).isEqualTo(ActivityType.UNSHARE);
            assertThat(dto.itemTitle()).isEqualTo("My Collection");
        });
    }

    @Test
    void stillRecordsWithoutATitleWhenTheTypeHasNoDescriptorProvider() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        final GRN grantee = grnRegistry.newGRN(GRNTypes.USER, "jane");
        when(grnDescriptorService.getDescriptor(entity))
                .thenThrow(new IllegalStateException("Missing GRN descriptor provider for GRN type: dashboard"));

        final var event = EntitySharesUpdateEvent.create(user, entity,
                List.of(EntitySharesUpdateEvent.Share.create(grantee, Capability.VIEW, null)),
                List.of(), List.of());

        assertThatCode(() -> listener.createRecentActivityFor(event)).doesNotThrowAnyException();

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService).save(captor.capture());
        assertThat(captor.getValue()).satisfies(dto -> {
            assertThat(dto.activityType()).isEqualTo(ActivityType.SHARE);
            assertThat(dto.itemTitle()).isNull();
        });
    }
}
