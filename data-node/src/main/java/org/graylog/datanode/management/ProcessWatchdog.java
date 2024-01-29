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
package org.graylog.datanode.management;

import org.graylog.datanode.process.FailuresCounter;
import org.graylog.datanode.process.ProcessEvent;
import org.graylog.datanode.process.ProcessState;
import org.graylog.datanode.process.StateMachineTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This process watchdog follows transitions of the state machine and will try to restart the process in case of termination.
 * If the process is actually stopped, it won't restart it and will automatically deactivate itself.
 */
public class ProcessWatchdog implements StateMachineTracer {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessWatchdog.class);

    private boolean active;
    private final FailuresCounter restartCounter;
    private final ManagableProcess<?> process;

    public ProcessWatchdog(ManagableProcess<?> process, int restartAttemptsCount) {
        this.process = process;
        this.restartCounter = FailuresCounter.zeroBased(restartAttemptsCount);
    }

    @Override
    public void trigger(ProcessEvent trigger) {
        LOG.debug("Watchdog trigger: {}", trigger);
    }

    @Override
    public void transition(ProcessEvent trigger, ProcessState source, ProcessState destination) {
        LOG.debug("Watchdog transition event:{}, source:{}, destination:{}", trigger, source, destination);
        switch (trigger) {
            case PROCESS_STARTED -> activateWatchdog();
            case PROCESS_TERMINATED -> restartProcess();
            case HEALTH_CHECK_OK -> resetCounter();
            case PROCESS_STOPPED -> deactivateWatchdog();
        }
    }

    private void resetCounter() {
        this.restartCounter.resetFailuresCounter();
    }

    private void activateWatchdog() {
        this.active = true;
    }

    private void deactivateWatchdog() {
        this.active = false;
    }

    private void restartProcess() {
        if (this.active) {
            if (!restartCounter.failedTooManyTimes()) {
                try {
                    LOG.info("Detected terminated process, restarting. Attempt #{}", restartCounter.failuresCount() + 1);
                    process.start();
                } catch (Exception e) {
                    LOG.warn("Failed to restart process", e);
                } finally {
                    restartCounter.increment();
                }
            } else {
                // give up trying, stop the watchdog
                LOG.warn("Process watchdog terminated after too many restart attempts");
                active = false;
            }
        }
    }

    public boolean isActive() {
        return active;
    }
}
