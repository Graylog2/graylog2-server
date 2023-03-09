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
package org.graylog2.plugin;

import org.graylog2.plugin.inputs.MessageInput;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Record failures from {@link MessageInput}s that happen during runtime.
 * It will toggle the {@link IOState} between {@code FAILING} and {@code RUNNING}
 * and also log the exception.
 * The InputFailureRecorder is usually passed into the Transport of Inputs
 * through {@link org.graylog2.plugin.inputs.transports.ThrottleableTransport2#doLaunch(MessageInput, InputFailureRecorder)}
 */
public class InputFailureRecorder {
    private final IOState<MessageInput> inputState;

    public InputFailureRecorder(IOState<MessageInput> inputState) {
        this.inputState = inputState;
    }

    /**
     * Set the input into the FAILING state.
     * @param loggingClass the calling class which will be used to log the error
     * @param error the error message
     */
    public void setFailing(Class<?> loggingClass, String error) {
        setFailing(loggingClass, error, null);
    }

    /**
     * Set the input into the FAILING state.
     * @param loggingClass the calling class which will be used to log the error
     * @param error the error message
     * @param e the exception leading to the error
     */
    public void setFailing(Class<?> loggingClass, String error, @Nullable Throwable e) {
        if (inputState.getState().equals(IOState.Type.FAILING)) {
            return;
        }
        if (e != null) {
            inputState.setState(IOState.Type.FAILING, error + ": (" + e.getMessage() + ")");
        } else {
            inputState.setState(IOState.Type.FAILING, error);
        }
        LoggerFactory.getLogger(loggingClass).warn(error, e);
    }

    /**
     * Set the input back into RUNNING state.
     * Call this once the error has resolved itself.
     */
    public void setRunning() {
        if (inputState.getState() == IOState.Type.RUNNING) {
            return;
        }
        inputState.setState(IOState.Type.RUNNING);
    }
}
