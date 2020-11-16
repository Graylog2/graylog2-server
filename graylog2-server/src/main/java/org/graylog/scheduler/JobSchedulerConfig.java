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
package org.graylog.scheduler;

/**
 * Used by the scheduler to configure itself.
 */
public interface JobSchedulerConfig {
    /**
     * Determines if the scheduler can start.
     *
     * @return true if the scheduler can be started, false otherwise
     */
    boolean canStart();

    /**
     * Determines if the scheduler can execute the next loop iteration.
     * This method will be called at the beginning of each scheduler loop iteration so it should be fast!
     *
     * @return true if scheduler can execute next loop iteration
     */
    boolean canExecute();

    /**
     * The number of worker threads to start.
     *
     * @return number of worker threads
     */
    int numberOfWorkerThreads();
}
