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

import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.grn.GRNRegistry;
import org.graylog.mcp.tools.PermissionHelper;
import org.graylog2.shared.security.RestPermissions;
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
class EventDefinitionResourceProviderTest {
    private static final String ID = "5f4dcc3b5aa765d61d8327de";

    @Mock
    private DBEventDefinitionService eventDefinitionService;
    @Mock
    private PermissionHelper permissionHelper;
    @Mock
    private EventDefinitionDto eventDefinition;

    private EventDefinitionResourceProvider provider;
    private URI uri;

    @BeforeEach
    void setUp() {
        provider = new EventDefinitionResourceProvider(eventDefinitionService, new CustomizationConfig(null), GRNRegistry.createWithBuiltinTypes());
        uri = URI.create(EventDefinitionResourceProvider.GRN_TYPE.toGRN(ID).toString());
    }

    @Test
    void readFallsBackToIdWhenTitleIsBlank() {
        // Regression guard: a blank-titled event definition must stay readable (SDK 2.0.0 rejects a
        // blank Resource name), so read() falls back to the id like list() does.
        when(permissionHelper.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, ID)).thenReturn(true);
        when(eventDefinitionService.get(ID)).thenReturn(Optional.of(eventDefinition));
        when(eventDefinition.title()).thenReturn("");
        when(eventDefinition.id()).thenReturn(ID);
        when(eventDefinition.description()).thenReturn("an event definition");

        assertThat(provider.read(permissionHelper, uri)).hasValueSatisfying(r -> {
            assertThat(r.name()).isEqualTo(ID);
            assertThat(r.uri()).isEqualTo(uri.toString());
            assertThat(r.description()).isEqualTo("an event definition");
        });
    }

    @Test
    void readUsesTitleWhenPresent() {
        when(permissionHelper.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, ID)).thenReturn(true);
        when(eventDefinitionService.get(ID)).thenReturn(Optional.of(eventDefinition));
        when(eventDefinition.title()).thenReturn("My Event Definition");
        when(eventDefinition.description()).thenReturn("an event definition");

        assertThat(provider.read(permissionHelper, uri))
                .hasValueSatisfying(r -> assertThat(r.name()).isEqualTo("My Event Definition"));
    }

    @Test
    void readReturnsEmptyWhenNotPermitted() {
        when(permissionHelper.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, ID)).thenReturn(false);
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }

    @Test
    void readReturnsEmptyWhenNotFound() {
        when(permissionHelper.isPermitted(RestPermissions.EVENT_DEFINITIONS_READ, ID)).thenReturn(true);
        when(eventDefinitionService.get(ID)).thenReturn(Optional.empty());
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }
}
