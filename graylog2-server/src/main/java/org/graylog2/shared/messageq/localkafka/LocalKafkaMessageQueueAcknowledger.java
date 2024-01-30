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
package org.graylog2.shared.messageq.localkafka;

import org.graylog2.plugin.Message;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.messageq.AbstractMessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Optional;

@Singleton
public class LocalKafkaMessageQueueAcknowledger extends AbstractMessageQueueAcknowledger<Long> {
    private final LocalKafkaJournal kafkaJournal;

    @Inject
    public LocalKafkaMessageQueueAcknowledger(LocalKafkaJournal kafkaJournal,
                                              MessageQueueAcknowledger.Metrics metrics) {
        super(Long.class, metrics);
        this.kafkaJournal = kafkaJournal;
    }

    @Override
    public void acknowledge(List<Message> messages) {
        @SuppressWarnings("ConstantConditions")
        final Optional<Long> max =
                messages.stream()
                        .map(Message::getMessageQueueId)
                        .filter(this::isValidMessageQueueId)
                        .map(Long.class::cast)
                        .max(Long::compare);
        max.ifPresent(this::doAcknowledge);
        metrics.acknowledgedMessages().mark(messages.size());
    }

    @Override
    protected void doAcknowledge(Long queueId) {
        kafkaJournal.markJournalOffsetCommitted(queueId);
    }
}
