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

public enum ProcessState {
    /**
     * Fresh created process, not started yet
     */
    NEW,
    /**
     * The process is running on the underlying OS and has a process ID. It's not responding to the REST API yet
     */
    STARTING,
    /**
     * Opensearch is now available on the default port with GREEN cluster status, all good
     */
    AVAILABLE,
    /**
     * There are problems in the REST communication with the opensearch. We'll retry several times before
     * we go to the FAILED state, giving up.
     */
    NOT_RESPONDING,

    /**
     * Failed to reach the opensearch REST API, but the underlying process is still alive
     */
    FAILED,
    /**
     * The OS process is not running anymore on the underlying system, it has been terminated
     */
    TERMINATED
}
