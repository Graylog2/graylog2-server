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
package org.graylog2.shared.messageq;

import org.graylog2.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AbstractMessageQueueAcknowledger<T> implements MessageQueueAcknowledger {
    private static final Logger log = LoggerFactory.getLogger(AbstractMessageQueueAcknowledger.class);

    protected final Class<T> queueIdClass;
    protected final Metrics metrics;

    public AbstractMessageQueueAcknowledger(Class<T> queueIdClass, Metrics metrics) {
        this.queueIdClass = queueIdClass;
        this.metrics = metrics;
    }

    @Override
    public void acknowledge(Object queueId) {
        if (isValidMessageQueueId(queueId)) {
            //noinspection unchecked
            doAcknowledge((T) queueId);
            metrics.acknowledgedMessages().mark();
        }
    }

    @Override
    public void acknowledge(Message message) {
        acknowledge(message.getMessageQueueId());
    }

    @Override
    public void acknowledge(List<Message> messages) {
        messages.forEach(message -> acknowledge(message.getMessageQueueId()));
    }

    protected abstract void doAcknowledge(T queueId);

    protected boolean isValidMessageQueueId(Object object) {
        if (queueIdClass.isInstance(object)) {
            return true;
        }
        // null is not valid, but it's also not an error condition because we might be dealing with a synthetic message
        // e.g. from the "create_message" pipeline function
        if (object != null) {
            log.error("{} is unable to acknowledge message. Expected <{}> to be of type <{}>, but found <{}>.",
                    getClass().getSimpleName(), object, queueIdClass.getSimpleName(), object.getClass().getSimpleName());
        }
        return false;
    }
}
