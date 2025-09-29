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
package org.graylog2.migrations;

import org.graylog.events.notifications.DBNotificationService;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.urlallowlist.RegexHelper;
import org.graylog2.system.urlallowlist.UrlAllowlist;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20191129134600_CreateInitialUrlAllowlistTest {
    @Mock
    private ClusterConfigService configService;
    @Mock
    private UrlAllowlistService allowlistService;
    @Mock
    private DBDataAdapterService dataAdapterService;
    @Mock
    private DBNotificationService notificationService;
    @Spy
    private RegexHelper regexHelper;

    @InjectMocks
    private V20191129134600_CreateInitialUrlAllowlist migration;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void createQuotedRegexEntry() {
        final HTTPJSONPathDataAdapter.Config config = mock(HTTPJSONPathDataAdapter.Config.class);
        when(config.url()).thenReturn("https://www.graylog.com/${key}/test.json/${key}");
        final DataAdapterDto dataAdapterDto = mock(DataAdapterDto.class);
        when(dataAdapterDto.config()).thenReturn(config);
        when(dataAdapterService.streamAll()).thenReturn(Stream.of(dataAdapterDto));

        migration.upgrade();

        final ArgumentCaptor<UrlAllowlist> captor = ArgumentCaptor.forClass(UrlAllowlist.class);
        verify(allowlistService).saveAllowlist(captor.capture());

        final UrlAllowlist allowlist = captor.getValue();

        final String allowlisted = "https://www.graylog.com/message/test.json/message";
        final String notAllowlisted = "https://wwwXgraylogXcom/message/testXjson/messsage";

        assertThat(allowlist.isAllowlisted(allowlisted)).withFailMessage(
                        "allowlist " + allowlist + " is expected to consider url <" + allowlisted + "> allowlisted.")
                .isTrue();
        assertThat(allowlist.isAllowlisted(notAllowlisted)).withFailMessage(
                        "allowlist " + allowlist + " is expected to consider url <" + notAllowlisted + "> not allowlisted.")
                .isFalse();
        assertThat(allowlist.entries()
                .size()).isEqualTo(1);
        assertThat(allowlist.entries()
                .get(0)
                .value()).isEqualTo("^\\Qhttps://www.graylog.com/\\E.*?\\Q/test.json/\\E.*?$");
    }
}
