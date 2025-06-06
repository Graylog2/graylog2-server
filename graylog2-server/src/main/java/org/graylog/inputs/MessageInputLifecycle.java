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
package org.graylog.inputs;

import com.google.inject.assistedinject.Assisted;
import jakarta.inject.Inject;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.InputFailureRecorder;
import org.graylog2.plugin.buffers.InputBuffer;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageInputLifecycle {
    public interface Factory {
        MessageInputLifecycle create(MessageInput input);
    }

    private static final Logger LOG = LoggerFactory.getLogger(MessageInputLifecycle.class);

    private final MessageInput input;
    private final InputBuffer inputBuffer;

    @Inject
    public MessageInputLifecycle(@Assisted MessageInput input,
                                 InputBuffer inputBuffer) {
        this.input = input;
        this.inputBuffer = inputBuffer;
    }

    public void start(IOState<MessageInput> inputState) {
        LOG.debug("Starting input {}", input.toIdentifier());

        try {
            input.checkConfiguration();
            input.initialize();
            input.launch(inputBuffer, new InputFailureRecorder(inputState));
            inputState.triggerRunning();
            LOG.debug("Completed starting input {}", input.toIdentifier());
        } catch (Exception e) {
            handleLaunchException(e, inputState);
        }
    }

    public void stop(IOState<MessageInput> inputState) {
        LOG.debug("Stopping input {}", input.toIdentifier());

        input.stop();
        inputState.triggerStopped();
        input.terminate();
        inputState.triggerTerminate();
        LOG.debug("Completed stopping input {}", input.toIdentifier());
    }

    protected void handleLaunchException(Throwable e, IOState<MessageInput> inputState) {
        StringBuilder msg = new StringBuilder("The [" + input.getClass().getCanonicalName() + "] input " + input.toIdentifier() + " misfired. Reason: ");

        String causeMsg = ExceptionUtils.getRootCauseMessage(e);

        msg.append(causeMsg);

        inputState.triggerFail(MessageInputFailure.asError(input.getClass(), msg.toString(), e));
    }
}
