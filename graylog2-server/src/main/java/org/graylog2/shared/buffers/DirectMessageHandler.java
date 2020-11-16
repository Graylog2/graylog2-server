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
package org.graylog2.shared.buffers;

import com.lmax.disruptor.WorkHandler;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.system.processing.ProcessingStatusRecorder;

import javax.inject.Inject;

class DirectMessageHandler implements WorkHandler<RawMessageEvent> {

    private final ProcessBuffer processBuffer;
    private final ProcessingStatusRecorder processingStatusRecorder;

    @Inject
    public DirectMessageHandler(ProcessBuffer processBuffer,
                                ProcessingStatusRecorder processingStatusRecorder) {
        this.processBuffer = processBuffer;
        this.processingStatusRecorder = processingStatusRecorder;
    }

    @Override
    public void onEvent(RawMessageEvent event) throws Exception {
        final RawMessage rawMessage = event.getRawMessage();
        processBuffer.insertBlocking(rawMessage);
        if (rawMessage != null) {
            processingStatusRecorder.updateIngestReceiveTime(rawMessage.getTimestamp());
        }
        // clear out for gc and to avoid promoting the raw message event to a tenured gen
        event.setRawMessage(null);
    }


}
