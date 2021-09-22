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
package org.graylog2.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerUpdate;

import javax.inject.Inject;

public class UserSessionTerminationJob implements Job {
    public static final String TYPE_NAME = "user-session-termination";

    private final UserSessionTerminationService sessionTerminationService;

    @Inject
    public UserSessionTerminationJob(UserSessionTerminationService sessionTerminationService) {
        this.sessionTerminationService = sessionTerminationService;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        sessionTerminationService.runGlobalSessionTermination();
        return JobTriggerUpdate.withoutNextTime();
    }

    public interface Factory extends Job.Factory<UserSessionTerminationJob> {
        @Override
        UserSessionTerminationJob create(JobDefinitionDto jobDefinition);
    }

    @JsonTypeName(UserSessionTerminationJob.TYPE_NAME)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobDefinitionConfig implements org.graylog.scheduler.JobDefinitionConfig {
        @Override
        public String type() {
            return UserSessionTerminationJob.TYPE_NAME;
        }
    }
}
