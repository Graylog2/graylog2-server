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
package org.graylog.datanode.opensearch.statemachine.tracer;

import jakarta.inject.Inject;
import org.graylog.datanode.opensearch.OpensearchProcess;
import org.graylog.datanode.opensearch.statemachine.FailuresCounter;
import org.graylog.datanode.opensearch.statemachine.OpensearchEvent;
import org.graylog.datanode.opensearch.statemachine.OpensearchState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This process watchdog follows transitions of the state machine and will try to restart the process in case of termination.
 * If the process is actually stopped, it won't restart it and will automatically deactivate itself.
 */
public class OpensearchWatchdog implements StateMachineTracer {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchWatchdog.class);

    private boolean active;
    private final FailuresCounter restartCounter;
    private final OpensearchProcess process;

    @Inject
    public OpensearchWatchdog(OpensearchProcess process) {
        this(process, 3);
    }

    public OpensearchWatchdog(OpensearchProcess process, int restartAttemptsCount) {
        this.process = process;
        this.restartCounter = FailuresCounter.zeroBased(restartAttemptsCount);
    }

    @Override
    public void trigger(OpensearchEvent trigger) {
        LOG.debug("Watchdog trigger: {}", trigger);
    }

    @Override
    public void transition(OpensearchEvent trigger, OpensearchState source, OpensearchState destination) {
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
