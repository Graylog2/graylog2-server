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

public class InputFailureRecorder {
    private final IOState<MessageInput> inputState;
    private final MessageInput input;

    public InputFailureRecorder(IOState<MessageInput> inputState, MessageInput input) {
        this.inputState = inputState;
        this.input = input;
    }

    @SuppressWarnings("rawtypes")
    public void isFailing(Class clazz, String error) {
        isFailing(clazz, error, null);
    }

    @SuppressWarnings("rawtypes")
    public void isFailing(Class clazz, String error, @Nullable Throwable e) {
        if (inputState.getState().equals(IOState.Type.FAILING)) {
            return;
        }
        if (e != null) {
            inputState.setState(IOState.Type.FAILING, error + ": (" + e.getMessage() + ")");
        } else {
            inputState.setState(IOState.Type.FAILING, error);
        }
        LoggerFactory.getLogger(clazz).warn(error, e);
    }

    public void isRunning() {
        if (inputState.getState() == IOState.Type.RUNNING) {
            return;
        }
        inputState.setState(IOState.Type.RUNNING);
    }
}
