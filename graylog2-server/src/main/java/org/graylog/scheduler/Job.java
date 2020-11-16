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
 * Interface to be implemented by job classes.
 */
public interface Job {
    interface Factory<TYPE extends Job> {
        TYPE create(JobDefinitionDto jobDefinition);
    }

    /**
     * Called by the scheduler when a trigger fires to execute the job. It returns a {@link JobTriggerUpdate} that
     * instructs the scheduler about the next trigger execution time, trigger data and others.
     *
     * @param ctx the job execution context
     * @return the trigger update
     * @throws JobExecutionException if the job execution fails
     */
    JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException;
}
