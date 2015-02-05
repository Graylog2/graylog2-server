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
package org.graylog2.shared.journal;

import com.google.common.util.concurrent.AbstractIdleService;

import java.util.List;

/**
 * NoopJournal is used when disk journalling is turned off. In order to avoid propagating the knowledge about whether
 * journalling is happening or not, we inject a no-op journal.
 * <p><strong>Any use</strong> of this journal will throw an IllegalStateException.</p>
 */
public class NoopJournal extends AbstractIdleService implements Journal {

    @Override
    public Entry createEntry(byte[] idBytes, byte[] messageBytes) {
        return new Entry(idBytes, messageBytes);
    }

    @Override
    public long write(List<Entry> entries) {
        throw new IllegalStateException("Invalid use of NoopJournal. Writing to this journal is always a programming error.");
    }

    @Override
    public long write(byte[] idBytes, byte[] messageBytes) {
        throw new IllegalStateException("Invalid use of NoopJournal. Writing to this journal is always a programming error.");
    }

    @Override
    public List<JournalReadEntry> read(long maximumCount) {
        throw new IllegalStateException("Invalid use of NoopJournal. Reading from this journal is always a programming error.");
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
