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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    }

    @Test
    void sharingAndUnsharingStoreTheResolvedTitleAndResolveItOnlyOnce() {
        when(user.getFullName()).thenReturn("Jane Doe");
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        final GRN jane = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final GRN john = grnRegistry.newGRN(GRNTypes.USER, "john");
        final GRN team = grnRegistry.newGRN(GRNTypes.BUILTIN_TEAM, "ops");
        when(grnDescriptorService.getDescriptor(entity)).thenReturn(GRNDescriptor.create(entity, "My Dashboard"));

        listener.createRecentActivityFor(EntitySharesUpdateEvent.create(user, entity,
                List.of(share(jane), share(john), share(jane)),
                List.of(share(team)),
                List.of()));

        // Resolved once per event, not once per row.
        verify(grnDescriptorService, times(1)).getDescriptor(entity);

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService, times(3)).save(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(dto -> {
            assertThat(dto.itemGrn()).isEqualTo(entity);
            assertThat(dto.itemTitle()).isEqualTo("My Dashboard");
            assertThat(dto.userName()).isEqualTo("Jane Doe");
        });
        // The duplicate grantee in creates() is filtered out.
        assertThat(captor.getAllValues())
                .extracting(RecentActivityDTO::activityType, RecentActivityDTO::grantee)
                .containsExactly(
                        tuple(ActivityType.SHARE, jane.toString()),
                        tuple(ActivityType.SHARE, john.toString()),
                        tuple(ActivityType.UNSHARE, team.toString()));
    }

    @Test
    void storesNoTitleWhenTheGrnTypeHasNoDescriptorProvider() {
        when(user.getFullName()).thenReturn("Jane Doe");
        // Outputs are shareable (content pack installs share them) but have no registered descriptor provider, so
        // getDescriptor really throws here instead of being stubbed to.
        final var listenerWithoutProviders = new RecentActivityUpdatesListener(new EventBus(), recentActivityService,
                new GRNDescriptorService(Map.of()));
        final GRN entity = grnRegistry.newGRN(GRNTypes.OUTPUT, "1");
        final var event = EntitySharesUpdateEvent.create(user, entity,
                List.of(share(grnRegistry.newGRN(GRNTypes.USER, "jane"))), List.of(), List.of());

        assertThatCode(() -> listenerWithoutProviders.createRecentActivityFor(event)).doesNotThrowAnyException();

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService).save(captor.capture());
        assertThat(captor.getValue().activityType()).isEqualTo(ActivityType.SHARE);
        assertThat(captor.getValue().itemTitle()).isNull();
    }

    @Test
    void storesThePlaceholderTitleWhenTheProviderCannotResolveTheEntity() {
        when(user.getFullName()).thenReturn("Jane Doe");
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");
        // Providers report a missing entity with a placeholder title instead of throwing, and we deliberately store it
        // as-is: filtering it would blank legitimate titles that happen to look like a placeholder.
        when(grnDescriptorService.getDescriptor(entity)).thenReturn(GRNDescriptor.empty(entity));

        listener.createRecentActivityFor(EntitySharesUpdateEvent.create(user, entity,
                List.of(share(grnRegistry.newGRN(GRNTypes.USER, "jane"))), List.of(), List.of()));

        final var captor = ArgumentCaptor.forClass(RecentActivityDTO.class);
        verify(recentActivityService).save(captor.capture());
        assertThat(captor.getValue().itemTitle()).isEqualTo(entity.toString());
    }

    @Test
    void recordsNothingAndResolvesNoTitleWhenOnlyCapabilitiesChanged() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "1");

        listener.createRecentActivityFor(EntitySharesUpdateEvent.create(user, entity,
                List.of(), List.of(), List.of(share(grnRegistry.newGRN(GRNTypes.USER, "jane")))));

        verifyNoInteractions(recentActivityService, grnDescriptorService);
    }

    private EntitySharesUpdateEvent.Share share(GRN grantee) {
        return EntitySharesUpdateEvent.Share.create(grantee, Capability.VIEW, null);
    }
}
