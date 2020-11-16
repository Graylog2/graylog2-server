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
package org.graylog2.system.urlwhitelist;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlWhitelistServiceTest {
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
