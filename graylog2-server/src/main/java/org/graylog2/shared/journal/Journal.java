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
package org.graylog2.shared.journal;

import java.util.List;
import java.util.Optional;

public interface Journal {
    Entry createEntry(byte[] idBytes, byte[] messageBytes);

    long write(List<Entry> entries);

    long write(byte[] idBytes, byte[] messageBytes);

    List<JournalReadEntry> read(long maximumCount);

    /**
     * Read from the journal, starting at the given offset. If the underlying journal implementation returns an empty
     * list of entries, it will be returned even if we know there are more entries in the journal.
     *
     * @param readOffset            Offset to start reading at
     * @param requestedMaximumCount Maximum number of entries to return.
     * @return A list of entries
     */
    List<JournalReadEntry> read(long readOffset, long requestedMaximumCount);

    void markJournalOffsetCommitted(long offset);

    /**
     * Returns the highest journal offset that has been written to persistent storage by Graylog.
     * <p>
     * Every message at an offset prior to this one can be considered as processed and does not need to be held in
     * the journal any longer. By default, Graylog will try to aggressively flush the journal to consume a smaller
     * amount of disk space.
     * </p>
     *
     * @return the offset of the last message which has been successfully processed.
     */
    long getCommittedOffset();

    /**
     * Returns the next offset the client should read.
     * <p>
     * This offset is *not* the committed offset (see {@link #getCommittedOffset()} for that), it just keeps track
     * of the message this client has already processed without telling the journal that all the read messages have
     * been processed successfully.
     * </p><p>
     * Caution: Do not use the {@code nextReadOffset} with more than one consumer!
     * </p>
     *
     * @return The offset of the next message to consume.
     */
    long getNextReadOffset();

    /**
     * Sets the next read offset the client should start reading from.
     * To allow the client to simply continue reading, it can set this method in case of an error to
     * {@code getCommittedOffset + 1} and just do its thing.
     *
     * @param nextReadOffset The next offset to continue reading from.
     */
    void setNextReadOffset(long nextReadOffset);

    void flush();

    /**
     * Returns an {@code Optional} containing the current journal utilization as a percentage of the
     * maximum retention size. This default implementation returns an empty {@code Optional},
     * indicating that no utilization data is available.
     *
     * @return an {@code Optional<Double>} representing the journal utilization percentage,
     * or an empty {@code Optional} if utilization data is unavailable.
     */
    default Optional<Double> getJournalUtilization() {
        return Optional.empty();
    }

    /**
     * Executes the retention policy on the journal, deleting outdated or excess data based on the
     * configured retention rules.
     *
     * @return an integer representing the amount of data deleted during the retention process.
     */
    int runRetention();

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
