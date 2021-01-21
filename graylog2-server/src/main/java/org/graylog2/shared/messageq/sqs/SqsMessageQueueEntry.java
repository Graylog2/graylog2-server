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

import com.amazonaws.services.sqs.model.Message;
import com.google.common.io.BaseEncoding;
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class SqsMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] encodedRawMessage;
    private final byte[] messageId;
    private final String receiptHandle;

    public static MessageQueue.Entry fromMessage(Message message) {
        return new SqsMessageQueueEntry(message);
    }

    private SqsMessageQueueEntry(Message message) {
        this.encodedRawMessage = BaseEncoding.base64().omitPadding().decode(message.getBody());
        this.messageId = message.getMessageId().getBytes(StandardCharsets.UTF_8);
        this.receiptHandle = message.getReceiptHandle();
    }

    @Override
    public String commitId() {
        return receiptHandle;
    }

    @Override
    public byte[] id() {
        return this.messageId;
    }

    @Nullable
    @Override
    public byte[] key() {
        return null;
    }

    @Override
    public byte[] value() {
        return encodedRawMessage;
    }

    @Override
    public Optional<Long> timestamp() {
        return Optional.empty();
    }
}
