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

import org.graylog.scheduler.JobExecutionContext;
import org.graylog2.plugin.Tools;

/**
 * Context information and utilities for system jobs.
 */
public class SystemJobContext {
    private final JobExecutionContext ctx;

    public SystemJobContext(JobExecutionContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Checks if the job has been cancelled.
     *
     * @return true if the job is cancelled, false otherwise
     */
    public boolean isCancelled() {
        return ctx.isCancelled();
    }

    /**
     * Update the progress of the job. The progress value represents the percent completion of the job and
     * should be between 0 and 100. Every call of this method executes a database update, so it should not be called
     * too frequently.
     *
     * @param progress the progress percentage to set (0-100)
     * @see #updateProgress(long, long)
     */
    public void updateProgress(int progress) {
        ctx.updateProgress(Math.clamp(progress, 0, 100));
    }

    /**
     * Update the progress of the job based on total and completed work units. Every call of this method executes
     * a database update, so it should not be called too frequently.
     *
     * @param total     the total number of work units
     * @param completed the number of completed work units
     */
    public void updateProgress(long total, long completed) {
        updateProgress(Tools.percentageOfRounded(total, completed));
    }
}
