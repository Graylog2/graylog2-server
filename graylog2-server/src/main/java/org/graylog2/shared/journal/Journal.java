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

import java.util.List;

public interface Journal {
    Entry createEntry(byte[] idBytes, byte[] messageBytes);

    long write(List<Entry> entries);

    long write(byte[] idBytes, byte[] messageBytes);

    List<JournalReadEntry> read(long maximumCount);

    void markJournalOffsetCommitted(long offset);

    class Entry {
        private final byte[] idBytes;
        private final byte[] messageBytes;

        public Entry(byte[] idBytes, byte[] messageBytes) {
            this.idBytes = idBytes;
            this.messageBytes = messageBytes;
        }

        public byte[] getIdBytes() {
            return idBytes;
        }

        public byte[] getMessageBytes() {
            return messageBytes;
        }
    }

    class JournalReadEntry {

        private final byte[] payload;
        private final long offset;

        public JournalReadEntry(byte[] payload, long offset) {
            this.payload = payload;
            this.offset = offset;
        }

        public long getOffset() {
            return offset;
        }

        public byte[] getPayload() {
            return payload;
        }
    }
}
