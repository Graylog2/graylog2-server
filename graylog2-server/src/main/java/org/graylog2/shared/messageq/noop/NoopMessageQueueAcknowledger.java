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
package org.graylog2.shared.messageq.noop;

import org.graylog2.plugin.Message;
import org.graylog2.shared.messageq.MessageQueueAcknowledger;

import jakarta.inject.Singleton;

import java.util.List;

@Singleton
public class NoopMessageQueueAcknowledger implements MessageQueueAcknowledger {

    @Override
    public void acknowledge(Object messageId) {
    }

    @Override
    public void acknowledge(Message message) {
    }

    @Override
    public void acknowledge(List<Message> messages) {
    }
}
