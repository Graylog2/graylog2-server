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

import com.google.common.util.concurrent.AbstractService;
import org.graylog2.shared.buffers.RawMessageEvent;
import org.graylog2.shared.messageq.MessageQueueException;
import org.graylog2.shared.messageq.MessageQueueWriter;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class NoopMessageQueueWriter extends AbstractService implements MessageQueueWriter {

    @Override
    protected void doStart() {
    }

    @Override
    protected void doStop() {
    }

    @Override
    public void write(List<RawMessageEvent> entries) throws MessageQueueException {
        throw new UnsupportedOperationException();
    }
}
