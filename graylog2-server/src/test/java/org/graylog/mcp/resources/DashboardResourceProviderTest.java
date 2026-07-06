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
package org.graylog.mcp.resources;

import org.graylog.grn.GRNRegistry;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardResourceProviderTest {
    private static final String ID = "5f4dcc3b5aa765d61d8327de";

    @Mock
    private ViewService viewService;
    @Mock
    private PermissionHelper permissionHelper;
    @Mock
    private SearchUser searchUser;
    @Mock
    private ViewDTO dashboard;

    private DashboardResourceProvider provider;
    private URI uri;

    @BeforeEach
    void setUp() {
        provider = new DashboardResourceProvider(viewService, new CustomizationConfig(null), GRNRegistry.createWithBuiltinTypes());
        uri = URI.create(DashboardResourceProvider.GRN_TYPE.toGRN(ID).toString());
    }

    @Test
    void readFallsBackToIdWhenTitleIsBlank() {
        // Regression guard: a blank-titled dashboard must stay readable (SDK 2.0.0 rejects a blank
        // Resource name), so read() falls back to the id like list() does.
        when(viewService.get(ID)).thenReturn(Optional.of(dashboard));
        when(permissionHelper.getSearchUser()).thenReturn(searchUser);
        when(searchUser.canReadView(dashboard)).thenReturn(true);
        when(dashboard.title()).thenReturn("");
        when(dashboard.id()).thenReturn(ID);
        when(dashboard.description()).thenReturn("a dashboard");

        assertThat(provider.read(permissionHelper, uri)).hasValueSatisfying(r -> {
            assertThat(r.name()).isEqualTo(ID);
            assertThat(r.uri()).isEqualTo(uri.toString());
            assertThat(r.description()).isEqualTo("a dashboard");
        });
    }

    @Test
    void readUsesTitleWhenPresent() {
        when(viewService.get(ID)).thenReturn(Optional.of(dashboard));
        when(permissionHelper.getSearchUser()).thenReturn(searchUser);
        when(searchUser.canReadView(dashboard)).thenReturn(true);
        when(dashboard.title()).thenReturn("My Dashboard");
        when(dashboard.description()).thenReturn("a dashboard");

        assertThat(provider.read(permissionHelper, uri))
                .hasValueSatisfying(r -> assertThat(r.name()).isEqualTo("My Dashboard"));
    }

    @Test
    void readReturnsEmptyWhenNotFound() {
        when(viewService.get(ID)).thenReturn(Optional.empty());
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }

    @Test
    void readReturnsEmptyWhenNotPermitted() {
        when(viewService.get(ID)).thenReturn(Optional.of(dashboard));
        when(permissionHelper.getSearchUser()).thenReturn(searchUser);
        when(searchUser.canReadView(dashboard)).thenReturn(false);
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }
}
