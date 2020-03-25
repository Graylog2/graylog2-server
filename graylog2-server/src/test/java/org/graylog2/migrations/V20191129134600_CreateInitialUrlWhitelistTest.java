/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.migrations;

import org.graylog.events.notifications.DBNotificationService;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.system.urlwhitelist.RegexHelper;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class V20191129134600_CreateInitialUrlWhitelistTest {
    @Mock
    private ClusterConfigService configService;
    @Mock
    private UrlWhitelistService whitelistService;
    @Mock
    private DBDataAdapterService dataAdapterService;
    @Mock
    private DBNotificationService notificationService;
    @Spy
    private RegexHelper regexHelper;

    @InjectMocks
    private V20191129134600_CreateInitialUrlWhitelist migration;

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
        when(dataAdapterService.findAll()).thenReturn(Collections.singleton(dataAdapterDto));

        migration.upgrade();

        final ArgumentCaptor<UrlWhitelist> captor = ArgumentCaptor.forClass(UrlWhitelist.class);
        verify(whitelistService).saveWhitelist(captor.capture());

        final UrlWhitelist whitelist = captor.getValue();

        final String whitelisted = "https://www.graylog.com/message/test.json/message";
        final String notWhitelisted = "https://wwwXgraylogXcom/message/testXjson/messsage";

        assertThat(whitelist.isWhitelisted(whitelisted)).withFailMessage(
                "Whitelist " + whitelist + " is expected to consider url <" + whitelisted + "> whitelisted.")
                .isTrue();
        assertThat(whitelist.isWhitelisted(notWhitelisted)).withFailMessage(
                "Whitelist " + whitelist + " is expected to consider url <" + notWhitelisted + "> not whitelisted.")
                .isFalse();
        assertThat(whitelist.entries()
                .size()).isEqualTo(1);
        assertThat(whitelist.entries()
                .get(0)
                .value()).isEqualTo("^\\Qhttps://www.graylog.com/\\E.*?\\Q/test.json/\\E.*?$");
    }
}
