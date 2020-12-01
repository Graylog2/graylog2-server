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

import com.google.common.util.concurrent.AbstractService;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueReader;

import javax.inject.Inject;
import java.util.List;

// TODO this doesn't do anything but to please the interface. remove?
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
        return null;
    }

    @Override
    public void commit(Object messageId) throws MessageQueueException {

    }
}
