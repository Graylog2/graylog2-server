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

import org.graylog.scheduler.JobSchedule;

import java.util.Optional;

/**
 * Provides a schedule for a scheduled {@link SystemJob}.
 */
public interface SystemJobScheduleProvider<CONFIG extends SystemJobConfig> {
    /**
     * Returns the schedule for the scheduled system job. An empty {@link Optional} signals that the job should
     * not be scheduled.
     *
     * @return the schedule
     * @throws Exception when an error occurs
     */
    Optional<JobSchedule> getSchedule() throws Exception;

    CONFIG getConfig();
}
