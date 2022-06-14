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
package org.graylog.scheduler.rest;

import com.google.common.collect.ImmutableList;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.security.UserContext;

import java.util.List;
import java.util.Optional;

/**
 * Pluggable interface for common job scheduler resource tasks.
 * Implementations are responsible on checking the permissions of the {@link UserContext}
 */
public interface JobResourceHandler {

    // TODO should we expose the UserContext into the plugin API like this? Maybe introduce an interface...
    default List<JobTriggerDto> listAllJobs(UserContext userContext) {
        return ImmutableList.of();
    }

    default Optional<JobTriggerDto> getJob(UserContext userContext, String jobID) {
        return Optional.empty();
    }

    default Optional<JobTriggerDto> cancelJob(UserContext userContext, String jobId) {
        return Optional.empty();
    }

    default JobTriggerDetails getTriggerDetails(JobTriggerDto triggerDto) {
        return JobTriggerDetails.EMPTY_DETAILS;
    }
}
