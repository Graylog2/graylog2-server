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
package org.graylog2.shared.messageq.sqs;

import org.graylog2.shared.messageq.MessageQueueAcknowledger;

import javax.inject.Inject;
import java.util.List;

public class SqsMessageQueueAcknowledger implements MessageQueueAcknowledger {
    private SqsMessageQueueReader sqsMessageQueueReader;

    @Inject
    public SqsMessageQueueAcknowledger(SqsMessageQueueReader sqsMessageQueueReader) {
        this.sqsMessageQueueReader = sqsMessageQueueReader;
    }

    @Override
    public void acknowledge(Object messageId) {
        sqsMessageQueueReader.commit(messageId);
    }

    @Override
    public void acknowledge(List<Object> messageIds) {
        sqsMessageQueueReader.commit(messageIds);
    }
}
