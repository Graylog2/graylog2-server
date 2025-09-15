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
package org.graylog2.system.urlallowlist;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlAllowlistServiceTest {
    @InjectMocks
    UrlAllowlistService urlAllowlistService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void addEntry() {
        final AllowlistEntry existingEntry = LiteralAllowlistEntry.create("a", "a", "a");
        final UrlAllowlist existingAllowlist = UrlAllowlist.createEnabled(Collections.singletonList(existingEntry));
        final AllowlistEntry newEntry = LiteralAllowlistEntry.create("b", "b", "b");

        final UrlAllowlist allowlistWithEntryAdded = urlAllowlistService.addEntry(existingAllowlist, newEntry);
        assertThat(allowlistWithEntryAdded).isEqualTo(
                UrlAllowlist.createEnabled(ImmutableList.of(existingEntry, newEntry)));

        final AllowlistEntry replacedEntry = LiteralAllowlistEntry.create("a", "c", "c");

        final UrlAllowlist allowlistWithEntryReplaced =
                urlAllowlistService.addEntry(allowlistWithEntryAdded, replacedEntry);
        assertThat(allowlistWithEntryReplaced).isEqualTo(
                UrlAllowlist.createEnabled(ImmutableList.of(replacedEntry, newEntry)));

    }

    @Test
    public void removeEntry() {
        final AllowlistEntry entry = LiteralAllowlistEntry.create("a", "a", "a");
        final UrlAllowlist allowlist = UrlAllowlist.createEnabled(Collections.singletonList(entry));
        assertThat(urlAllowlistService.removeEntry(allowlist, null)).isEqualTo(allowlist);
        assertThat(urlAllowlistService.removeEntry(allowlist, "b")).isEqualTo(allowlist);
        assertThat(urlAllowlistService.removeEntry(allowlist, "a")).isEqualTo(
                UrlAllowlist.createEnabled(Collections.emptyList()));
    }
}
