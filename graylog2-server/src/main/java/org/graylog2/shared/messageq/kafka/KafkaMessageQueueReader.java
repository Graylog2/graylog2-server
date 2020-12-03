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
package org.graylog2.shared.messageq.kafka;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AbstractService;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.messageq.MessageQueue;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueReader;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.transform;

public class KafkaMessageQueueReader extends AbstractService implements MessageQueueReader {
    private KafkaJournal kafkaJournal;

    @Inject
    public KafkaMessageQueueReader(KafkaJournal kafkaJournal) {
        this.kafkaJournal = kafkaJournal;
    }

    @Override
    protected void doStart() {

    }

    @Override
    protected void doStop() {

    }

    @Override
    public List<Entry> read(long maximumCount) {
        final List<Journal.JournalReadEntry> entries = kafkaJournal.read(maximumCount);
        final ArrayList<Entry> messageQueueEntries = Lists.newArrayList(transform(entries, new MessageJournalConverter()));
        return messageQueueEntries;
    }

    @Override
    public void commit(Object messageId) throws MessageQueueException {

    }

    @Override
    public Entry createEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp) {
        return null;
    }

    private class MessageJournalConverter implements Function<Journal.JournalReadEntry, MessageQueue.Entry> {
        @Nullable
        @Override
        public MessageQueue.Entry apply(@Nullable Journal.JournalReadEntry input) {
            return new KafkaMessageQueueEntry(input.getPayload(), input.getOffset());
        }
    }
}
