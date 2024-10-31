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
package org.graylog.datanode.process;

import org.apache.commons.exec.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This listener allows to be disabled from outside - if we manually stop the process, we want to stop all the incoming
 * events as well, otherwise we'll receive a process termination after the stop, which will then confuse our watchdog
 * and other parts of the datanode.
 */
public class CommandLineProcessListener implements ProcessListener {

    private static final Logger LOG = LoggerFactory.getLogger(CommandLineProcessListener.class);

    private final ProcessListener delegate;
    private boolean listening = true;

    public CommandLineProcessListener(ProcessListener delegate) {
        this.delegate = delegate;
    }

    public void stopListening() {
        this.listening = false;
    }


    @Override
    public void onProcessComplete(int exitValue) {
        if(listening) {
            delegate.onProcessComplete(exitValue);
        } else {
            LOG.info("Ignoring onProcessComplete({}) call, this process is already stopped", exitValue);
        }
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        if(listening) {
            delegate.onProcessFailed(e);
        } else {
            LOG.info("Ignoring onProcessFailed({}) call,  this process is already stopped", e.getMessage());
        }
    }

    @Override
    public void onStart() {
        if(listening) {
            delegate.onStart();
        } else {
            LOG.info("Ignoring onStart() call,  this process is already stopped");
        }
    }

    @Override
    public void onStdOut(String line) {
        if(listening) {
            delegate.onStdOut(line);
        }
    }

    @Override
    public void onStdErr(String line) {
        if(listening) {
            delegate.onStdErr(line);
        }
    }
}
