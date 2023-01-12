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

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OpensearchLogs {

    private final CircularFifoQueue<String> stdout = new CircularFifoQueue<>(500);
    private final CircularFifoQueue<String> stderr = new CircularFifoQueue<>(500);

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchLogs.class);

    public void stdOut(String line) {
        stdout.add(line);
        LOG.info(line);
    }

    public void stdErr(String line) {
        stderr.add(line);
        LOG.warn(line);
    }

    public List<String> getStdout() {
        return stdout.stream().toList();
    }

    public List<String> getStderr() {
        return stderr.stream().toList();
    }
}
