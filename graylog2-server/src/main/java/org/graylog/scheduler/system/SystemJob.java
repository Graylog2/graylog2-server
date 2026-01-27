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
package org.graylog.scheduler.system;

import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.graylog.scheduler.system.SystemJobResult.Converter.toJobTriggerUpdate;

/**
 * Interface to be implemented by system job classes.
 */
public interface SystemJob<CONFIG extends SystemJobConfig> extends Job {
    Logger LOG = LoggerFactory.getLogger(SystemJob.class);

    interface Factory<TYPE extends SystemJob<? extends SystemJobConfig>> {
        TYPE create();
    }

    SystemJobResult execute(CONFIG config, SystemJobContext ctx) throws JobExecutionException;

    default JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final var data = ctx.trigger().data()
                .orElseThrow(() -> new IllegalStateException("No trigger data available for system job execution."));

        LOG.debug("Executing system job: {}", data);
        //noinspection unchecked
        return toJobTriggerUpdate(execute((CONFIG) data, new SystemJobContext(ctx)), ctx.trigger());
    }
}
