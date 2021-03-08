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
import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
public class LocalKafkaMessageQueueAcknowledger implements MessageQueueAcknowledger {
    private static final Logger LOG = LoggerFactory.getLogger(LocalKafkaMessageQueueAcknowledger.class);
    private LocalKafkaJournal kafkaJournal;
    private final Metrics metrics;

    @Inject
    public LocalKafkaMessageQueueAcknowledger(LocalKafkaJournal kafkaJournal,
                                              MessageQueueAcknowledger.Metrics metrics) {
        this.kafkaJournal = kafkaJournal;
        this.metrics = metrics;
    }

    @Override
    public void acknowledge(Object offset) {
        doAcknowledge(offset);
        metrics.acknowledgedMessages().mark();
    }

    @Override
    public void acknowledge(Message message) {
        doAcknowledge(message.getMessageQueueId());
        metrics.acknowledgedMessages().mark();
    }

    @Override
    public void acknowledge(List<Message> messages) {
        final Optional<Long> max =
                messages.stream().map(Message::getMessageQueueId).filter(Long.class::isInstance).map(Long.class::cast).max(Long::compare);
        max.ifPresent(this::doAcknowledge);
        metrics.acknowledgedMessages().mark(messages.size());
    }

    private void doAcknowledge(Object object) {
        if (!(object instanceof Long)) {
            LOG.error("Couldn't acknowledge message. Expected <" + object + "> to be of type Long");
            return;
        }
        final long offset = (Long) object;
        kafkaJournal.markJournalOffsetCommitted(offset);
    }
}
