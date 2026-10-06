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
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;
import org.graylog2.web.customization.CustomizationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamResourceProviderTest {
    private static final String ID = "5f4dcc3b5aa765d61d8327de";

    @Mock
    private StreamService streamService;
    @Mock
    private PermissionHelper permissionHelper;
    @Mock
    private Stream stream;

    private StreamResourceProvider provider;
    private URI uri;

    @BeforeEach
    void setUp() {
        provider = new StreamResourceProvider(streamService, new CustomizationConfig(null), GRNRegistry.createWithBuiltinTypes());
        uri = URI.create(StreamResourceProvider.GRN_TYPE.toGRN(ID).toString());
    }

    @Test
    void readFallsBackToIdWhenTitleIsBlank() throws Exception {
        // Regression guard: the REST API allows a blank stream title, but SDK 2.0.0 rejects a blank
        // Resource name. read() must fall back to the id like list() does, so the entity stays readable.
        when(permissionHelper.isPermitted(RestPermissions.STREAMS_READ, ID)).thenReturn(true);
        when(streamService.load(ID)).thenReturn(stream);
        when(stream.getTitle()).thenReturn("");
        when(stream.getId()).thenReturn(ID);
        when(stream.getDescription()).thenReturn("a stream");

        assertThat(provider.read(permissionHelper, uri)).hasValueSatisfying(r -> {
            assertThat(r.name()).isEqualTo(ID);
            assertThat(r.uri()).isEqualTo(uri.toString());
            assertThat(r.description()).isEqualTo("a stream");
        });
    }

    @Test
    void readUsesTitleWhenPresent() throws Exception {
        when(permissionHelper.isPermitted(RestPermissions.STREAMS_READ, ID)).thenReturn(true);
        when(streamService.load(ID)).thenReturn(stream);
        when(stream.getTitle()).thenReturn("My Stream");
        when(stream.getDescription()).thenReturn("a stream");

        assertThat(provider.read(permissionHelper, uri))
                .hasValueSatisfying(r -> assertThat(r.name()).isEqualTo("My Stream"));
    }

    @Test
    void readReturnsEmptyWhenNotPermitted() {
        when(permissionHelper.isPermitted(RestPermissions.STREAMS_READ, ID)).thenReturn(false);
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }

    @Test
    void readReturnsEmptyWhenStreamNotFound() throws Exception {
        when(permissionHelper.isPermitted(RestPermissions.STREAMS_READ, ID)).thenReturn(true);
        when(streamService.load(ID)).thenThrow(new NotFoundException("not found"));
        assertThat(provider.read(permissionHelper, uri)).isEmpty();
    }
}
