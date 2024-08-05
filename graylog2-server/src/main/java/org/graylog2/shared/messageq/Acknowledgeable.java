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

import javax.annotation.Nullable;

/**
 * An object adhering to this interface can be acknowledged in a message queue by providing a queue-specific ID.
 * <p>
 * For example, messages that have been read from the local kafka journal will use a journal offset as message queue ID.
 * By using that offset to commit to the journal after the messages has been successfully indexed, the message can be
 * acknowledged in the queue.
 */
public interface Acknowledgeable {
    @Nullable
    Object getMessageQueueId();
}
