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
package org.graylog2.system.urlwhitelist;

import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlWhitelistServiceTest {
    @Mock
    ClusterConfigService clusterConfigService;

    @InjectMocks
    UrlWhitelistService urlWhitelistService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void addEntry() {
        final WhitelistEntry existingEntry = LiteralWhitelistEntry.create("a", "a", "a");
        final UrlWhitelist existingWhitelist = UrlWhitelist.createEnabled(Collections.singletonList(existingEntry));
        final WhitelistEntry newEntry = LiteralWhitelistEntry.create("b", "b", "b");

        final UrlWhitelist whitelistWithEntryAdded = urlWhitelistService.addEntry(existingWhitelist, newEntry);
        assertThat(whitelistWithEntryAdded).isEqualTo(
                UrlWhitelist.createEnabled(ImmutableList.of(existingEntry, newEntry)));

        final WhitelistEntry replacedEntry = LiteralWhitelistEntry.create("a", "c", "c");

        final UrlWhitelist whitelistWithEntryReplaced =
                urlWhitelistService.addEntry(whitelistWithEntryAdded, replacedEntry);
        assertThat(whitelistWithEntryReplaced).isEqualTo(
                UrlWhitelist.createEnabled(ImmutableList.of(replacedEntry, newEntry)));

    }

    @Test
    public void removeEntry() {
        final WhitelistEntry entry = LiteralWhitelistEntry.create("a", "a", "a");
        final UrlWhitelist whitelist = UrlWhitelist.createEnabled(Collections.singletonList(entry));
        assertThat(urlWhitelistService.removeEntry(whitelist, null)).isEqualTo(whitelist);
        assertThat(urlWhitelistService.removeEntry(whitelist, "b")).isEqualTo(whitelist);
        assertThat(urlWhitelistService.removeEntry(whitelist, "a")).isEqualTo(
                UrlWhitelist.createEnabled(Collections.emptyList()));
    }
}
