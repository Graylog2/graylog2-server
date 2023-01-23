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
package org.graylog.datanode;

import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.ExecuteException;
import org.graylog.datanode.process.OpensearchProcess;
import org.graylog.datanode.process.ProcessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventResultHandler extends DefaultExecuteResultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(EventResultHandler.class);

    private final OpensearchProcess process;

    public EventResultHandler(OpensearchProcess process) {
        this.process = process;
    }

    @Override
    public void onProcessComplete(int exitValue) {
        LOG.warn("Opensearch process terminated with exit value" + exitValue);
        process.onEvent(ProcessEvent.PROCESS_TERMINATED);
    }

    @Override
    public void onProcessFailed(ExecuteException e) {
        LOG.warn("Opensearch process failed", e);
        process.onEvent(ProcessEvent.PROCESS_TERMINATED);
    }
}
