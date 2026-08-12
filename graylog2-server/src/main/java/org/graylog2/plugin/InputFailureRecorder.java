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

    /**
     * Set once a failure that retrying cannot resolve has been recorded. Guarded by {@code this} together with every
     * state write, so that a concurrent {@link #setRunning()} cannot undo a terminal failure.
     */
    private boolean terminallyFailed = false;

    public InputFailureRecorder(IOState<MessageInput> inputState) {
        this.inputState = inputState;
    }

    /**
     * Set the input into the FAILING state.
     * <p>
     * Keeps the message of a failure that is already recorded: if the input is already FAILING, this call is a no-op.
     * Use {@link #setTerminallyFailing} when the new message must win.
     * @param loggingClass the calling class which will be used to log the error
     * @param error the error message
     */
    public void setFailing(Class<?> loggingClass, String error) {
        setFailing(loggingClass, error, null);
    }

    /**
     * Set the input into the FAILING state.
     * <p>
     * Keeps the message of a failure that is already recorded: if the input is already FAILING, this call is a no-op.
     * Use {@link #setTerminallyFailing} when the new message must win.
     * @param loggingClass the calling class which will be used to log the error
     * @param error the error message
     * @param e the exception leading to the error
     */
    public synchronized void setFailing(Class<?> loggingClass, String error, @Nullable Throwable e) {
        if (terminallyFailed || inputState.getState().equals(IOState.Type.FAILING)) {
            return;
        }
        applyFailure(loggingClass, error, e);
    }

    /**
     * Set the input into the FAILING state for a failure that retrying cannot resolve, replacing the message of any
     * failure already recorded and blocking any later {@link #setRunning()}.
     * <p>
     * Use this when the message carries the only actionable detail, so that it is neither hidden behind an earlier
     * transient message nor cleared by work that continues while the input drains. Like {@link #setFailing}, the first
     * message wins: a second unrecoverable failure does not replace it.
     * @param loggingClass the calling class which will be used to log the error
     * @param error the error message
     * @param e the exception leading to the error
     */
    public synchronized void setTerminallyFailing(Class<?> loggingClass, String error, @Nullable Throwable e) {
        if (terminallyFailed) {
            return;
        }
        terminallyFailed = true;
        applyFailure(loggingClass, error, e);
    }

    private void applyFailure(Class<?> loggingClass, String error, @Nullable Throwable e) {
        if (e != null) {
            inputState.setState(IOState.Type.FAILING, error + ": (" + (e.getMessage() != null ? e.getMessage() : e.toString()) + ")");
        } else {
            inputState.setState(IOState.Type.FAILING, error);
        }
        if (terminallyFailed) {
            // ERROR, not WARN: this is the line that explains why an error loop stopped and why the input is down.
            LoggerFactory.getLogger(loggingClass).error(error, e);
        } else {
            LoggerFactory.getLogger(loggingClass).warn(error, e);
        }
    }

    /**
     * Set the input back into RUNNING state.
     * Call this once the error has resolved itself. Has no effect after {@link #setTerminallyFailing}.
     */
    public synchronized void setRunning() {
        if (terminallyFailed || inputState.getState() == IOState.Type.RUNNING) {
            return;
        }
        inputState.setState(IOState.Type.RUNNING);
    }
}
