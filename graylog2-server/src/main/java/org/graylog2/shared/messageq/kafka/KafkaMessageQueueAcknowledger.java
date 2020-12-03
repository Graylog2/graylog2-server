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

import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueException;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class KafkaMessageQueueAcknowledger implements MessageQueueAcknowledger {
    private KafkaJournal kafkaJournal;

    @Inject
    public KafkaMessageQueueAcknowledger(KafkaJournal kafkaJournal) {
        this.kafkaJournal = kafkaJournal;
    }

    @Override
    public void acknowledge(Object messageId) throws MessageQueueException {
        if (messageId instanceof Long) {
            kafkaJournal.markJournalOffsetCommitted((Long) messageId);
        } else {
            throw new MessageQueueException("Couldn't acknowledge unknown message type <" + messageId + ">");
        }
    }

    @Override
    public void acknowledge(List<Object> messageIds) throws MessageQueueException {
        final Optional<Long> max = messageIds.stream().filter(Long.class::isInstance).map(Long.class::cast).max(Long::compare);
        if (max.isPresent()) {
            acknowledge(max.get());
        }
    }
}
