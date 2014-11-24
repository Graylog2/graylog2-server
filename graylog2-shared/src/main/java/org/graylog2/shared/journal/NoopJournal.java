/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.journal;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractIdleService;

import java.util.ArrayList;
import java.util.List;

/**
 * NoopJournal is used when disk journalling is turned off. In order to avoid propagating the knowledge about whether
 * journalling is happening or not, we inject a no-op journal.
 */
public class NoopJournal extends AbstractIdleService implements Journal {

    public static final ArrayList<JournalReadEntry> JOURNAL_READ_ENTRIES = Lists.newArrayList();

    @Override
    public Entry createEntry(byte[] idBytes, byte[] messageBytes) {
        return new Entry(idBytes, messageBytes);
    }

    @Override
    public long write(List<Entry> entries) {
        return Long.MIN_VALUE;
    }

    @Override
    public long write(byte[] idBytes, byte[] messageBytes) {
        return Long.MIN_VALUE;
    }

    @Override
    public List<JournalReadEntry> read() {
        return JOURNAL_READ_ENTRIES;
    }

    @Override
    public void markJournalOffsetCommitted(long offset) {
        // nothing to do
    }

    @Override
    protected void startUp() throws Exception {
        // nothing to do
    }

    @Override
    protected void shutDown() throws Exception {
        // nothing to do
    }
}
