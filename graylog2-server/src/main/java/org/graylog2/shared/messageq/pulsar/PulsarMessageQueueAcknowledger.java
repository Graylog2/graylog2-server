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
package org.graylog2.shared.messageq.pulsar;

import org.graylog2.shared.messageq.MessageQueueAcknowledger;
import org.graylog2.shared.messageq.MessageQueueException;

import javax.inject.Inject;
import java.util.List;

public class PulsarMessageQueueAcknowledger implements MessageQueueAcknowledger {
    private PulsarMessageQueueReader pulsarMessageQueueReader;

    @Inject
    public PulsarMessageQueueAcknowledger(PulsarMessageQueueReader pulsarMessageQueueReader) {
        this.pulsarMessageQueueReader = pulsarMessageQueueReader;
    }

    @Override
    public void acknowledge(Object messageId) throws MessageQueueException {
        pulsarMessageQueueReader.commit(messageId);
    }

    @Override
    public void acknowledge(List<Object> messageIds) throws MessageQueueException {
        for (Object messageId : messageIds) {
            pulsarMessageQueueReader.commit(messageId);
        }
    }
}
