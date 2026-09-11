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
package org.graylog.plugins.threatintel.migrations;

import org.graylog.plugins.threatintel.adapters.otx.OTXDataAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.urlallowlist.AllowlistEntry;
import org.graylog2.system.urlallowlist.LiteralAllowlistEntry;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20260827120000_AddOtxDefaultUrlToAllowlistTest {
    private AutoCloseable mocks;

    @Mock
    private ClusterConfigService clusterConfigService;
    @Mock
    private DBDataAdapterService dbDataAdapterService;
    @Mock
    private UrlAllowlistService urlAllowlistService;

    @InjectMocks
    private V20260827120000_AddOtxDefaultUrlToAllowlist migration;

    @BeforeEach
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    public void skipsWhenAlreadyCompleted() {
        when(clusterConfigService.get(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.class))
                .thenReturn(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.create(0));

        migration.upgrade();

        verify(dbDataAdapterService, never()).streamAll();
        verify(urlAllowlistService, never()).addEntry(any());
    }

    @Test
    public void doesNothingWhenNoOtxAdaptersExist() {
        when(clusterConfigService.get(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.class))
                .thenReturn(null);
        when(dbDataAdapterService.streamAll()).thenReturn(Stream.empty());

        migration.upgrade();

        verify(urlAllowlistService, never()).addEntry(any());
        verify(clusterConfigService).write(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.create(0));
    }

    @Test
    public void doesNothingWhenOtxAdapterUsesCustomUrl() {
        when(clusterConfigService.get(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.class))
                .thenReturn(null);

        final OTXDataAdapter.Config config = mock(OTXDataAdapter.Config.class);
        when(config.apiUrl()).thenReturn("https://my-custom-otx-instance.example.com");
        final DataAdapterDto dto = mock(DataAdapterDto.class);
        when(dto.config()).thenReturn(config);
        when(dbDataAdapterService.streamAll()).thenReturn(Stream.of(dto));

        migration.upgrade();

        verify(urlAllowlistService, never()).addEntry(any());
        verify(clusterConfigService).write(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.create(0));
    }

    @Test
    public void doesNothingWhenDefaultUrlAlreadyAllowlisted() {
        when(clusterConfigService.get(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.class))
                .thenReturn(null);

        final OTXDataAdapter.Config config = mock(OTXDataAdapter.Config.class);
        when(config.apiUrl()).thenReturn(OTXDataAdapter.DEFAULT_API_URL);
        final DataAdapterDto dto = mock(DataAdapterDto.class);
        when(dto.config()).thenReturn(config);
        when(dbDataAdapterService.streamAll()).thenReturn(Stream.of(dto));
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(true);

        migration.upgrade();

        verify(urlAllowlistService, never()).addEntry(any());
        verify(clusterConfigService).write(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.create(0));
    }

    @Test
    public void addsDefaultUrlToAllowlistWhenOtxAdapterExistsAndUrlNotAllowlisted() {
        when(clusterConfigService.get(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.class))
                .thenReturn(null);

        final OTXDataAdapter.Config config = mock(OTXDataAdapter.Config.class);
        when(config.apiUrl()).thenReturn(OTXDataAdapter.DEFAULT_API_URL);
        final DataAdapterDto dto = mock(DataAdapterDto.class);
        when(dto.config()).thenReturn(config);
        when(dbDataAdapterService.streamAll()).thenReturn(Stream.of(dto));
        when(urlAllowlistService.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).thenReturn(false);

        migration.upgrade();

        final ArgumentCaptor<AllowlistEntry> entryCaptor = ArgumentCaptor.forClass(AllowlistEntry.class);
        verify(urlAllowlistService).addEntry(entryCaptor.capture());

        final AllowlistEntry addedEntry = entryCaptor.getValue();
        assertThat(addedEntry).isInstanceOf(LiteralAllowlistEntry.class);
        assertThat(addedEntry.value()).isEqualTo(OTXDataAdapter.DEFAULT_API_URL);
        assertThat(addedEntry.isAllowlisted(OTXDataAdapter.DEFAULT_API_URL)).isTrue();

        verify(clusterConfigService).write(V20260827120000_AddOtxDefaultUrlToAllowlist.MigrationCompleted.create(1));
    }
}
